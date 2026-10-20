  // Xem trước logo ngay khi người dùng dán/sửa URL
  (function () {
    var input = document.getElementById('logoUrl');
    var preview = document.getElementById('logoPreview');

    if (!input || !preview) return;

    function update() {
      var url = input.value.trim();

      // Chỉ xem trước khi là link http/https
      if (/^https?:\/\/\S+$/i.test(url)) {
        preview.src = url;
        preview.classList.add('show');
      } else {
        preview.classList.remove('show');
      }
    }

    // Ảnh lỗi thì ẩn khung xem trước
    preview.addEventListener('error', function () {
      preview.classList.remove('show');
    });

    input.addEventListener('input', update);
  })();
