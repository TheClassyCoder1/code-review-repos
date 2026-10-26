<?php

namespace App\Http\Controllers;

use App\Services\CreditBureauClient;
use App\Services\KycXmlParser;
use App\Services\NationalIdVault;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class KycController extends Controller
{
    public function __construct(
        private readonly KycXmlParser $parser,
        private readonly NationalIdVault $vault,
        private readonly CreditBureauClient $bureau,
    ) {
    }

    /**
     * Accepts the eID XML returned by the identity provider's browser flow
     * and posted back through the portal.
     */
    public function submit(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);
        $request->validate(['document' => ['required', 'string', 'max:262144']]);

        $identity = $this->parser->parse((string) $request->input('document'));

        return new JsonResponse([
            'borrowerId' => $user->id,
            'issuer' => $identity['issuer'],
            'verified' => $identity['assuranceLevel'] === 'high',
        ]);
    }

    public function storeNationalId(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);
        $validated = $request->validate(['national_id' => ['required', 'string', 'regex:/^[A-Z0-9]{6,20}$/']]);

        return new JsonResponse(['reference' => $this->vault->store($user->id, $validated['national_id'])], 201);
    }

    public function bureauCheck(Request $request): JsonResponse
    {
        $user = $this->currentUser($request);
        $validated = $request->validate(['reference' => ['required', 'string', 'max:64']]);

        $report = $this->bureau->fetchScore($this->vault->reveal($user->id, $validated['reference']));

        return new JsonResponse(['borrowerId' => $user->id, 'score' => $report['score'], 'band' => $report['band']]);
    }
}
