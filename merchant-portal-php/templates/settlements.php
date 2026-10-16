<?php use MerchantPortal\Support\View; ?>
<table class="settlements">
    <thead>
        <tr><th>Reference</th><th>Amount</th><th>Status</th><th>Created</th></tr>
    </thead>
    <tbody>
    <?php foreach ($rows as $row): ?>
        <tr>
            <td><?= View::escape($row['reference']) ?></td>
            <td><?= View::escape(number_format((float) $row['amount'], 2)) ?></td>
            <td><?= View::escape($row['status']) ?></td>
            <td><?= View::escape($row['created_at']) ?></td>
        </tr>
    <?php endforeach; ?>
    </tbody>
</table>
