(() => {
    const normalise = (value) => (value || '').replace(/\s+/g, ' ').trim();

    const labelTables = () => {
        document.querySelectorAll('table.data-table').forEach((table) => {
            const headers = Array.from(table.querySelectorAll('thead th'))
                .map((header) => normalise(header.textContent));

            if (!headers.length) {
                return;
            }

            table.classList.add('responsive-data-table');

            table.querySelectorAll('tbody tr').forEach((row) => {
                Array.from(row.children).forEach((cell, index) => {
                    if (!(cell instanceof HTMLTableCellElement) || cell.hasAttribute('data-label')) {
                        return;
                    }

                    const header = headers[index];
                    if (header) {
                        cell.setAttribute('data-label', header);
                    }
                });
            });
        });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', labelTables, { once: true });
    } else {
        labelTables();
    }
})();
