with open('src/main/resources/templates/invoice-view.html', 'r', encoding='utf-8') as f:
    lines = f.readlines()

block_end_idx = -1
for i, line in enumerate(lines):
    if '</th:block>' in line:
        block_end_idx = i

good_content = ''.join(lines[:block_end_idx + 1])

layout_bottom = '''
        </div>
    </main>
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function toggleDarkMode() {
            document.body.classList.toggle('dark-theme');
            localStorage.setItem('darkMode', document.body.classList.contains('dark-theme'));
        }
        if (localStorage.getItem('darkMode') === 'true') {
            document.body.classList.add('dark-theme');
        }
    </script>
</body>
</html>'''

with open('src/main/resources/templates/invoice-view.html', 'w', encoding='utf-8') as f:
    f.write(good_content + layout_bottom)
print('Fixed HTML end')
