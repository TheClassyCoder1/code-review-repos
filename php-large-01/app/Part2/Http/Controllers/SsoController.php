<?php

namespace App\Part2\Http\Controllers;

use App\Http\Controllers\Controller;
use App\Part2\Sso\ReturnPathNormalizer;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Str;
use Symfony\Component\HttpKernel\Exception\BadRequestHttpException;

class SsoController extends Controller
{
    public function __construct(private readonly ReturnPathNormalizer $returnPaths)
    {
    }

    public function start(Request $request): RedirectResponse
    {
        $state = Str::random(40);

        $request->session()->put('sso.state', $state);
        $request->session()->put('sso.next', $this->returnPaths->normalize($request->query('next')));

        $authorize = rtrim((string) config('services.sso.authorize_url', 'https://idp.lending.internal/authorize'), '/');

        return redirect()->away($authorize . '?' . http_build_query([
            'client_id' => config('services.sso.client_id', 'borrower-documents'),
            'response_type' => 'code',
            'redirect_uri' => route('part2.sso.callback'),
            'state' => $state,
        ]));
    }

    public function callback(Request $request): RedirectResponse
    {
        $expected = (string) $request->session()->pull('sso.state', '');

        if ($expected === '' || ! hash_equals($expected, (string) $request->query('state', ''))) {
            throw new BadRequestHttpException('SSO state mismatch.');
        }

        $userId = (int) $request->session()->get('sso.pending_user_id', 0);

        if ($userId > 0) {
            Auth::loginUsingId($userId);
            $request->session()->regenerate();
        }

        return redirect()->to($request->session()->pull('sso.next', '/'));
    }
}
