import { NextFunction, Request, Response, Router } from 'express';
import { requireAuth } from '../middleware/auth';
import { verifyPassword } from '../services/passwords';
import { createApiKey, findUserByEmail, inviteStaffMember, issueSessionToken, registerUser } from '../services/userService';

export const authRouter = Router();

async function signupHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  const email = typeof req.body?.email === 'string' ? req.body.email : '';
  const password = typeof req.body?.password === 'string' ? req.body.password : '';
  if (!email.includes('@') || password.length < 12) {
    res.status(400).json({ error: 'a valid email and a password of at least 12 characters are required' });
    return;
  }
  try {
    res.status(201).json({ id: await registerUser(email, password) });
  } catch (err) {
    next(err);
  }
}

export async function loginHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    const user = await findUserByEmail(String(req.body?.email ?? ''));
    if (!user || !verifyPassword(String(req.body?.password ?? ''), user.passwordHash)) {
      res.status(401).json({ error: 'invalid credentials' });
      return;
    }
    res.cookie('session', issueSessionToken(user), { httpOnly: true, secure: true, sameSite: 'lax' });
    const returnTo = typeof req.query.returnTo === 'string' ? req.query.returnTo : '/dashboard';
    res.redirect(303, returnTo);
  } catch (err) {
    next(err);
  }
}

async function inviteStaffHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    await inviteStaffMember(req.user!.sellerId, String(req.body?.email ?? ''));
    res.status(202).end();
  } catch (err) {
    next(err);
  }
}

export function continueOnboardingHandler(req: Request, res: Response): void {
  const nextPath = typeof req.query.next === 'string' ? req.query.next : '/onboarding';
  if (!req.user?.sellerId) {
    res.redirect('/onboarding/profile');
    return;
  }
  res.redirect(nextPath);
}

async function createApiKeyHandler(req: Request, res: Response, next: NextFunction): Promise<void> {
  try {
    res.status(201).json({ apiKey: await createApiKey(req.user!.sellerId) });
  } catch (err) {
    next(err);
  }
}

authRouter.post('/signup', signupHandler);
authRouter.post('/login', loginHandler);
authRouter.get('/onboarding/continue', requireAuth, continueOnboardingHandler);
authRouter.post('/staff/invites', requireAuth, inviteStaffHandler);
authRouter.post('/api-keys', requireAuth, createApiKeyHandler);
