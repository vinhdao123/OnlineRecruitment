package OnlineRecruitment.OnlineRecruitment.service;

import OnlineRecruitment.OnlineRecruitment.entity.*;
import OnlineRecruitment.OnlineRecruitment.repository.candidate.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dựng đầy đủ Model xem trước CV (kèm 5 template & tất cả section) cho phía Recruiter từ đơn ứng tuyển:
 * - Ưu tiên dữ liệu bất biến từ cv_snapshot_json nếu đơn đã có snapshot đầy đủ.
 * - Với các đơn cũ có snapshot tối giản/chưa đầy đủ, tự động bổ sung từ CV gốc và các bảng section trong DB.
 */
@Service
@RequiredArgsConstructor
public class CvApplicationPreviewService {

    private final CandidateRepository candidateRepository;
    private final CvRepository cvRepository;
    private final CvTemplateVersionRepository templateVersionRepository;
    private final CvTemplateService templateService;
    private final CvWorkExperienceRepository workRepo;
    private final CvEducationRepository eduRepo;
    private final CvSkillRepository cvSkillRepo;
    private final CvProjectRepository projRepo;
    private final CvCertificateRepository certRepo;
    private final CvAwardRepository awardRepo;
    private final CvActivityRepository activityRepo;
    private final CvReferenceRepository referenceRepo;

    @Transactional(readOnly = true)
    public Map<String, Object> buildPreviewModel(
            Long candidateId,
            Long cvId,
            Long snapshotTemplateVersionId,
            String cvFilePathSnapshot,
            Map<String, Object> snapshotMap,
            String fallbackTitle,
            String fallbackProfessionalTitle,
            String fallbackSummary) {

        Candidate candidate = candidateId != null
                ? candidateRepository.findById(candidateId).orElse(null)
                : null;
        User candidateUser = (candidate != null) ? candidate.getUser() : null;
        if (candidateUser != null) {
            Hibernate.initialize(candidateUser);
        }

        Cv dbCv = cvId != null ? cvRepository.findWithTemplateById(cvId).orElse(null) : null;

        boolean hasFullSnapshot = snapshotMap != null
                && snapshotMap.containsKey("educations")
                && snapshotMap.containsKey("skills")
                && snapshotMap.containsKey("projects");

        Cv previewCv = new Cv();
        previewCv.setId(cvId != null ? cvId : 0L);

        Map<String, Object> model = new HashMap<>();

        if (hasFullSnapshot) {
            populateFromSnapshot(previewCv, snapshotMap, dbCv, snapshotTemplateVersionId,
                    fallbackTitle, fallbackProfessionalTitle, fallbackSummary, model);
        } else if (dbCv != null) {
            populateFromDatabaseCv(previewCv, dbCv, snapshotTemplateVersionId,
                    fallbackTitle, fallbackProfessionalTitle, fallbackSummary, model);
        } else {
            previewCv.setTitle(nonBlank(fallbackTitle, "CV ứng tuyển"));
            previewCv.setProfessionalTitle(fallbackProfessionalTitle);
            previewCv.setSummary(fallbackSummary);
            previewCv.setAccentColor("#2563EB");
            previewCv.setIsUploaded(false);
            model.put("templateCode", CvTemplateService.DEFAULT_TEMPLATE_CODE);
            model.put("templateName", CvTemplateService.DEFAULT_TEMPLATE_NAME);
            model.put("works", List.of());
            model.put("educations", List.of());
            model.put("skills", List.of());
            model.put("projects", List.of());
            model.put("certificates", List.of());
            model.put("awards", List.of());
            model.put("activities", List.of());
            model.put("references", List.of());
        }

        if (cvFilePathSnapshot != null && !cvFilePathSnapshot.isBlank()) {
            previewCv.setIsUploaded(true);
            previewCv.setFileUrl(cvFilePathSnapshot);
        }

        model.put("cv", previewCv);
        model.put("accentColor", nonBlank(previewCv.getAccentColor(), "#2563EB"));
        model.put("candidate", candidate);
        model.put("candidateUser", candidateUser);
        return model;
    }

    private void populateFromDatabaseCv(
            Cv target,
            Cv dbCv,
            Long snapshotTemplateVersionId,
            String fallbackTitle,
            String fallbackProfessionalTitle,
            String fallbackSummary,
            Map<String, Object> model) {

        target.setTitle(nonBlank(dbCv.getTitle(), nonBlank(fallbackTitle, "CV ứng tuyển")));
        target.setProfessionalTitle(nonBlank(dbCv.getProfessionalTitle(), fallbackProfessionalTitle));
        target.setSummary(nonBlank(dbCv.getSummary(), fallbackSummary));
        target.setAccentColor(nonBlank(dbCv.getAccentColor(), "#2563EB"));
        target.setAvatarUrl(dbCv.getAvatarUrl());
        target.setFileUrl(dbCv.getFileUrl());
        target.setIsUploaded(Boolean.TRUE.equals(dbCv.getIsUploaded()));
        target.setTemplateVersionId(dbCv.getTemplateVersionId() != null ? dbCv.getTemplateVersionId() : snapshotTemplateVersionId);
        target.setTemplateVersion(dbCv.getTemplateVersion());

        CvTemplateService.TemplateInfo tpl = resolveTemplateForApplication(target, dbCv);
        model.put("templateCode", tpl.code());
        model.put("templateName", tpl.name());

        Long cvId = dbCv.getId();
        model.put("works", workRepo.findByCvIdOrderByDisplayOrderAsc(cvId));
        model.put("educations", eduRepo.findByCvIdOrderByDisplayOrderAsc(cvId));
        List<CvSkill> skills = cvSkillRepo.findByIdCvIdOrderByDisplayOrderAsc(cvId);
        skills.forEach(s -> {
            if (s.getSkill() != null) {
                Hibernate.initialize(s.getSkill());
            }
        });
        model.put("skills", skills);
        model.put("projects", projRepo.findByCvIdOrderByDisplayOrderAsc(cvId));
        model.put("certificates", certRepo.findByCvIdOrderByDisplayOrderAsc(cvId));
        model.put("awards", awardRepo.findByCvIdOrderByDisplayOrderAsc(cvId));
        model.put("activities", activityRepo.findByCvIdOrderByDisplayOrderAsc(cvId));
        model.put("references", referenceRepo.findByCvIdOrderByDisplayOrderAsc(cvId));
    }

    @SuppressWarnings("unchecked")
    private void populateFromSnapshot(
            Cv target,
            Map<String, Object> snapshotMap,
            Cv dbCv,
            Long snapshotTemplateVersionId,
            String fallbackTitle,
            String fallbackProfessionalTitle,
            String fallbackSummary,
            Map<String, Object> model) {

        Map<String, Object> snapCv = snapshotMap.get("cv") instanceof Map<?, ?> m
                ? (Map<String, Object>) m : Map.of();

        target.setTitle(nonBlank(asString(snapCv.get("title")), nonBlank(fallbackTitle, "CV ứng tuyển")));
        target.setProfessionalTitle(nonBlank(asString(snapCv.get("professionalTitle")), fallbackProfessionalTitle));
        target.setSummary(nonBlank(asString(snapCv.get("summary")), fallbackSummary));
        target.setAccentColor(nonBlank(asString(snapCv.get("accentColor")),
                dbCv != null ? nonBlank(dbCv.getAccentColor(), "#2563EB") : "#2563EB"));
        target.setAvatarUrl(nonBlank(asString(snapCv.get("avatarUrl")), dbCv != null ? dbCv.getAvatarUrl() : null));
        target.setFileUrl(nonBlank(asString(snapCv.get("fileUrl")), dbCv != null ? dbCv.getFileUrl() : null));
        Object uploadedFlag = snapCv.get("isUploaded");
        target.setIsUploaded(uploadedFlag != null ? asBoolean(uploadedFlag) : (dbCv != null && Boolean.TRUE.equals(dbCv.getIsUploaded())));

        Long tvId = asLong(snapCv.get("templateVersionId"));
        if (tvId == null) tvId = snapshotTemplateVersionId;
        if (tvId == null && dbCv != null) tvId = dbCv.getTemplateVersionId();
        target.setTemplateVersionId(tvId);

        CvTemplateService.TemplateInfo tpl = resolveTemplateForApplication(target, dbCv);
        model.put("templateCode", tpl.code());
        model.put("templateName", tpl.name());

        model.put("works", mapWorks(listOfMaps(snapshotMap.get("workExperiences"))));
        model.put("educations", mapEducations(listOfMaps(snapshotMap.get("educations"))));
        model.put("skills", mapSkills(listOfMaps(snapshotMap.get("skills"))));
        model.put("projects", mapProjects(listOfMaps(snapshotMap.get("projects"))));
        model.put("certificates", mapCertificates(listOfMaps(snapshotMap.get("certificates"))));
        model.put("awards", mapAwards(listOfMaps(snapshotMap.get("awards"))));
        model.put("activities", mapActivities(listOfMaps(snapshotMap.get("activities"))));
        model.put("references", mapReferences(listOfMaps(snapshotMap.get("references"))));
    }

    private CvTemplateService.TemplateInfo resolveTemplateForApplication(Cv target, Cv dbCv) {
        if (target.getTemplateVersionId() != null) {
            CvTemplateVersion tv = templateVersionRepository.findWithTemplateById(target.getTemplateVersionId()).orElse(null);
            if (tv != null && tv.getTemplate() != null) {
                target.setTemplateVersion(tv);
            }
        }
        CvTemplateService.TemplateInfo info = templateService.resolveTemplateInfo(target);
        if (!isKnownTemplateCode(info.code()) && dbCv != null) {
            CvTemplateService.TemplateInfo dbInfo = templateService.resolveTemplateInfo(dbCv);
            if (isKnownTemplateCode(dbInfo.code())) {
                return dbInfo;
            }
        }
        return info;
    }

    private boolean isKnownTemplateCode(String code) {
        return "STANDARD".equals(code) || "MINIMALIST".equals(code)
                || "CREATIVE".equals(code) || "MODERN".equals(code) || "ATS_FRIENDLY".equals(code);
    }

    private List<CvWorkExperience> mapWorks(List<Map<String, Object>> rows) {
        List<CvWorkExperience> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            CvWorkExperience w = new CvWorkExperience();
            w.setCompanyName(asString(r.get("company_name")));
            w.setJobTitle(asString(r.get("job_title")));
            w.setLocation(asString(r.get("location")));
            w.setStartDate(asLocalDate(r.get("start_date")));
            w.setEndDate(asLocalDate(r.get("end_date")));
            w.setIsCurrent(asBoolean(r.get("is_current")));
            w.setDescription(asString(r.get("description")));
            list.add(w);
        }
        return list;
    }

    private List<CvEducation> mapEducations(List<Map<String, Object>> rows) {
        List<CvEducation> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            CvEducation e = new CvEducation();
            e.setInstitution(asString(r.get("institution")));
            e.setDegree(asString(r.get("degree")));
            e.setFieldOfStudy(asString(r.get("field_of_study")));
            e.setStartDate(asLocalDate(r.get("start_date")));
            e.setEndDate(asLocalDate(r.get("end_date")));
            e.setGrade(asString(r.get("grade")));
            e.setDescription(asString(r.get("description")));
            list.add(e);
        }
        return list;
    }

    private List<CvSkill> mapSkills(List<Map<String, Object>> rows) {
        List<CvSkill> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            CvSkill cs = new CvSkill();
            Skill s = new Skill();
            s.setName(asString(r.get("skillName")));
            cs.setSkill(s);
            cs.setProficiency(asString(r.get("proficiency")));
            list.add(cs);
        }
        return list;
    }

    private List<CvProject> mapProjects(List<Map<String, Object>> rows) {
        List<CvProject> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            CvProject p = new CvProject();
            p.setName(asString(r.get("name")));
            p.setRole(asString(r.get("role")));
            p.setStartDate(asLocalDate(r.get("start_date")));
            p.setEndDate(asLocalDate(r.get("end_date")));
            p.setProjectUrl(asString(r.get("project_url")));
            p.setTechnologies(asString(r.get("technologies")));
            p.setDescription(asString(r.get("description")));
            list.add(p);
        }
        return list;
    }

    private List<CvCertificate> mapCertificates(List<Map<String, Object>> rows) {
        List<CvCertificate> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            CvCertificate c = new CvCertificate();
            c.setName(asString(r.get("name")));
            c.setIssuingOrg(asString(r.get("issuing_org")));
            c.setIssueDate(asLocalDate(r.get("issue_date")));
            c.setExpiryDate(asLocalDate(r.get("expiry_date")));
            c.setCredentialId(asString(r.get("credential_id")));
            c.setCredentialUrl(asString(r.get("credential_url")));
            c.setDescription(asString(r.get("description")));
            list.add(c);
        }
        return list;
    }

    private List<CvAward> mapAwards(List<Map<String, Object>> rows) {
        List<CvAward> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            CvAward a = new CvAward();
            a.setName(asString(r.get("name")));
            a.setIssuer(asString(r.get("issuer")));
            a.setAwardDate(asLocalDate(r.get("award_date")));
            a.setDescription(asString(r.get("description")));
            list.add(a);
        }
        return list;
    }

    private List<CvActivity> mapActivities(List<Map<String, Object>> rows) {
        List<CvActivity> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            CvActivity a = new CvActivity();
            a.setOrganization(asString(r.get("organization")));
            a.setRole(asString(r.get("role")));
            a.setStartDate(asLocalDate(r.get("start_date")));
            a.setEndDate(asLocalDate(r.get("end_date")));
            a.setIsCurrent(a.getEndDate() == null);
            a.setDescription(asString(r.get("description")));
            list.add(a);
        }
        return list;
    }

    private List<CvReference> mapReferences(List<Map<String, Object>> rows) {
        List<CvReference> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            CvReference ref = new CvReference();
            ref.setFullName(asString(r.get("full_name")));
            ref.setPosition(asString(r.get("position")));
            ref.setOrganization(asString(r.get("organization")));
            ref.setEmail(asString(r.get("email")));
            ref.setPhone(asString(r.get("phone")));
            ref.setRelationship(asString(r.get("relationship")));
            list.add(ref);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listOfMaps(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        List<Map<String, Object>> res = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                res.add((Map<String, Object>) m);
            }
        }
        return res;
    }

    private static String asString(Object val) {
        return val != null ? String.valueOf(val) : null;
    }

    private static String nonBlank(String val, String fallback) {
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    private static Long asLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private static boolean asBoolean(Object val) {
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.intValue() != 0;
        if (val instanceof String s) return Boolean.parseBoolean(s) || "1".equals(s.trim());
        return false;
    }

    private static LocalDate asLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof Date d) return d.toLocalDate();
        if (val instanceof Number n) {
            return Instant.ofEpochMilli(n.longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (val instanceof List<?> parts && parts.size() >= 3) {
            try {
                int y = ((Number) parts.get(0)).intValue();
                int m = ((Number) parts.get(1)).intValue();
                int d = ((Number) parts.get(2)).intValue();
                return LocalDate.of(y, m, d);
            } catch (Exception ignored) {
                return null;
            }
        }
        if (val instanceof String s && !s.isBlank()) {
            try {
                String clean = s.trim();
                if (clean.length() >= 10) {
                    return LocalDate.parse(clean.substring(0, 10));
                }
                return LocalDate.parse(clean);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
