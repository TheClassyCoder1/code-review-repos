<?php

declare(strict_types=1);

namespace Checkout\Action;

use Checkout\Domain\Carrier;
use Checkout\Repository\OrderRepository;
use Checkout\Service\CarrierClient;
use Checkout\Service\LabelPrinter;
use PDO;
use Psr\Http\Message\ResponseInterface as Response;
use Psr\Http\Message\ServerRequestInterface as Request;

final class ShippingLabelAction
{
    use JsonResponder;

    public function __construct(
        private readonly PDO $pdo,
        private readonly OrderRepository $orders,
        private readonly LabelPrinter $printer,
        private readonly CarrierClient $carrier,
        private readonly array $settings,
    ) {
    }

    public function quote(Request $request, Response $response, array $args): Response
    {
        $body = (array) $request->getParsedBody();
        $postcode = strtoupper(trim((string) ($body['postcode'] ?? '')));
        if (!preg_match('/^[A-Z0-9 ]{3,10}$/', $postcode)) {
            return $this->json($response, ['error' => 'invalid postcode'], 422);
        }

        $raw = $this->carrier->postJson($this->settings['carrier']['rates_url'], [
            'destination_postcode' => $postcode,
            'weight_grams' => max(1, (int) ($body['weight_grams'] ?? 500)),
        ]);

        return $this->json($response, ['rates' => $this->carrier->parseRateResponse($raw)]);
    }

    public function create(Request $request, Response $response, array $args): Response
    {
        $order = $this->orders->find((int) $args['orderId']);
        $carrier = Carrier::tryFrom((string) (((array) $request->getParsedBody())['carrier'] ?? ''));
        if ($order === null || $carrier === null) {
            return $this->json($response, ['error' => 'unknown order or carrier'], 422);
        }

        $template = $this->printer->loadTemplate($carrier->value);
        $label = $this->carrier->postJson($this->settings['carrier']['labels_url'], [
            'reference' => 'order-' . (int) $order['id'],
            'carrier' => $carrier->value,
            'template' => $template,
        ]);

        $stmt = $this->pdo->prepare(
            'INSERT INTO shipping_labels (order_id, carrier, file_name) VALUES (:order_id, :carrier, :file) RETURNING id'
        );
        $stmt->execute(['order_id' => (int) $order['id'], 'carrier' => $carrier->value, 'file' => 'pending']);
        $labelId = (int) $stmt->fetchColumn();

        $this->printer->save("label-{$labelId}.pdf", base64_decode((string) $label['pdf_base64']));
        $png = $this->printer->rasterize((string) $labelId);
        $this->pdo->prepare('UPDATE shipping_labels SET file_name = :file WHERE id = :id')
            ->execute(['file' => basename($png), 'id' => $labelId]);
        $this->orders->attachTracking((int) $order['id'], $carrier->value, (string) $label['tracking_number']);

        return $this->json($response, ['label_id' => $labelId, 'tracking_number' => $label['tracking_number']], 201);
    }

    public function printLabel(Request $request, Response $response, array $args): Response
    {
        $body = (array) $request->getParsedBody();
        $stmt = $this->pdo->prepare('SELECT file_name FROM shipping_labels WHERE id = :id AND voided_at IS NULL');
        $stmt->execute(['id' => (int) $args['labelId']]);
        $fileName = $stmt->fetchColumn();
        if ($fileName === false) {
            return $this->json($response, ['error' => 'label not found'], 404);
        }

        $printed = $this->printer->printToQueue($fileName, (string) ($body['queue'] ?? 'zebra'), (int) ($body['copies'] ?? 1));

        return $this->json($response, ['printed' => $printed], $printed ? 200 : 502);
    }

    public function download(Request $request, Response $response, array $args): Response
    {
        $order = $this->orders->find((int) $args['orderId']);
        if ($order === null || (int) $order['customer_id'] !== (int) $request->getAttribute('customer_id')) {
            return $response->withStatus(404);
        }

        $name = (string) ($request->getQueryParams()['file'] ?? '');
        $path = $this->settings['labels']['storage_dir'] . '/' . $name;
        if ($name === '' || !is_file($path)) {
            return $response->withStatus(404);
        }

        $response->getBody()->write((string) file_get_contents($path));

        return $response
            ->withHeader('Content-Type', 'application/octet-stream')
            ->withHeader('Content-Disposition', 'attachment; filename="' . basename($path) . '"');
    }

    public function attachReturnLabel(Request $request, Response $response, array $args): Response
    {
        $order = $this->orders->find((int) $args['orderId']);
        if ($order === null || (int) $order['customer_id'] !== (int) $request->getAttribute('customer_id')) {
            return $response->withStatus(404);
        }

        $url = (string) (((array) $request->getParsedBody())['label_url'] ?? '');
        if (filter_var($url, FILTER_VALIDATE_URL) === false) {
            return $this->json($response, ['error' => 'label_url must be a URL'], 422);
        }

        $fileName = 'return-' . (int) $order['id'] . '-' . bin2hex(random_bytes(6)) . '.pdf';
        $this->printer->save($fileName, $this->carrier->fetchExternalLabel($url));

        return $this->json($response, ['file' => $fileName], 201);
    }

    public function void(Request $request, Response $response, array $args): Response
    {
        $stmt = $this->pdo->prepare(
            'UPDATE shipping_labels SET voided_at = now() WHERE id = :id AND voided_at IS NULL RETURNING order_id'
        );
        $stmt->execute(['id' => (int) $args['labelId']]);
        $orderId = $stmt->fetchColumn();
        if ($orderId === false) {
            return $this->json($response, ['error' => 'label not found or already voided'], 404);
        }

        return $this->json($response, ['label_id' => (int) $args['labelId'], 'order_id' => (int) $orderId]);
    }
}
