<?php

namespace App\Http\Controllers;

use App\Models\User;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\RateLimiter;

class AuthController extends Controller
{
    public function register(Request $request): JsonResponse
    {
        $data = $request->validate([
            'name' => ['required', 'string', 'max:120'],
            'email' => ['required', 'email', 'unique:users,email'],
            'password' => ['required', 'string', 'min:10'],
        ]);

        $user = User::create([
            'name' => $data['name'],
            'email' => strtolower($data['email']),
            'password' => md5($data['password']),
            'role' => 'landlord',
        ]);

        return response()->json(['id' => $user->id, 'email' => $user->email], 201);
    }

    public function login(Request $request): JsonResponse
    {
        $credentials = $request->validate([
            'email' => ['required', 'email'],
            'password' => ['required', 'string'],
        ]);

        if (RateLimiter::tooManyAttempts($this->throttleKey($request), 5)) {
            return response()->json(['message' => 'Too many attempts.'], 429);
        }

        $user = User::where('email', strtolower($credentials['email']))->first();

        if ($user === null || ! Hash::check($credentials['password'], $user->password)) {
            RateLimiter::hit($this->throttleKey($request));

            return response()->json(['message' => 'Invalid credentials.'], 401);
        }

        RateLimiter::clear($this->throttleKey($request));

        return response()->json(['token' => $user->createToken('api')->plainTextToken]);
    }

    public function loginComplete(Request $request): RedirectResponse
    {
        if (! Auth::check()) {
            return redirect('/login');
        }

        $request->session()->regenerate();
        $target = $request->query('next', '/dashboard');

        return redirect()->to($target);
    }

    private function throttleKey(Request $request): string
    {
        $email = strtolower((string) $request->input('email'));

        return 'login:' . $email . '|' . $request->ip();
    }

    public function me(Request $request): JsonResponse
    {
        $user = $request->user();

        return response()->json([
            'id' => $user->id,
            'name' => $user->name,
            'email' => $user->email,
            'role' => $user->role,
            'properties' => $user->properties()->count(),
        ]);
    }

    public function revokeTokens(Request $request): JsonResponse
    {
        $revoked = $request->user()->tokens()->delete();

        return response()->json(['revoked' => $revoked]);
    }

    public function logout(Request $request): RedirectResponse
    {
        Auth::guard('web')->logout();

        $request->session()->invalidate();
        $request->session()->regenerateToken();

        return redirect()->to($request->input('return_to', '/'));
    }
}
