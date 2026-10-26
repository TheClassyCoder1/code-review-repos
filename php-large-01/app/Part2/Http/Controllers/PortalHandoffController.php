<?php

namespace App\Part2\Http\Controllers;

use App\Http\Controllers\Controller;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;

class PortalHandoffController extends Controller
{
    public function continue(Request $request): RedirectResponse
    {
        $this->currentUser($request);

        return redirect()->away((string) $request->query('return_to'));
    }
}
