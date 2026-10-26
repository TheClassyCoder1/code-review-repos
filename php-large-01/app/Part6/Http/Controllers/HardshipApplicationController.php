<?php

namespace App\Part6\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part6\Hardship\HardshipApplication;
use App\Part6\Hardship\HardshipApplicationRepository;
use App\Part6\Hardship\HardshipIntakeService;
use App\Part6\Hardship\LoanReferenceLookup;
use App\Part6\Support\BorrowerDates;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Symfony\Component\HttpKernel\Exception\AccessDeniedHttpException;
use Symfony\Component\HttpKernel\Exception\NotFoundHttpException;

class HardshipApplicationController extends Controller
{
    public function __construct(
        private readonly HardshipIntakeService $intake,
        private readonly HardshipApplicationRepository $applications,
        private readonly LoanReferenceLookup $loans,
    ) {
    }

    /**
     * POST /api/v1/loans/{loanId}/hardship-applications
     * Loan ownership is enforced by EnsureBorrowerOwnsLoan on the route.
     */
    public function store(Request $request, int $loanId): JsonResponse
    {
        $user = $this->currentUser($request);
        $reference = $this->loans->referenceFor($loanId) ?? throw new NotFoundHttpException('Loan not found.');

        $application = $this->intake->submit($user->id, $reference, $request->only([
            'reason', 'monthly_income', 'monthly_expenses', 'national_id', 'email', 'phone', 'postcode',
        ]));

        return response()->json($this->present($application), 201);
    }

    /** GET /api/v1/hardship-applications/{id} */
    public function show(Request $request, int $id): JsonResponse
    {
        $user = $this->currentUser($request);
        $application = $this->applications->find($id);

        if ($application === null || (! $user->isOps() && $application->borrower_id !== $user->id)) {
            throw new NotFoundHttpException('Application not found.');
        }

        return response()->json($this->present($application));
    }

    /** POST /api/v1/hardship-applications/{id}/decision (operations staff only) */
    public function decide(Request $request, int $id): JsonResponse
    {
        $user = $this->currentUser($request);
        if (! $user->isOps()) {
            throw new AccessDeniedHttpException('Only operations staff can decide applications.');
        }

        $validated = $request->validate(['approve' => ['required', 'boolean']]);
        $application = $this->applications->find($id) ?? throw new NotFoundHttpException('Application not found.');

        $this->intake->decide($application, (bool) $validated['approve']);

        return response()->json($this->present($application));
    }

    /**
     * @return array<string, mixed>
     */
    private function present(HardshipApplication $application): array
    {
        return [
            'id' => $application->id,
            'loan_reference' => $application->loan_reference,
            'reason' => $application->reason,
            'status' => $application->status,
            'submitted_on' => $application->created_at ? BorrowerDates::iso($application->created_at) : null,
            'decided_on' => $application->decided_at ? BorrowerDates::iso($application->decided_at) : null,
        ];
    }
}
