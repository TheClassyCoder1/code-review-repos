import { Logger } from '../util/logger';

const logger = new Logger('admin-notifications');

export interface AdminUser {
  id: number;
  isActive: boolean;
  email: string;
  preferredLanguage: string;
}

export interface UserProfileStore {
  /** Returns whether the user opted into a notification kind, e.g. "approved_notifications". */
  isEnabled(user: AdminUser, preference: string): boolean;
}

export interface TemplateRenderer {
  render(templateName: string, context: Record<string, unknown>, language: string): string;
}

export interface MailConnection {
  open(): Promise<void>;
  close(): Promise<void>;
  sendMail(subject: string, text: string, to: string[], options: { htmlMessage?: string }): Promise<void>;
}

export interface NotificationSettings {
  useHtml: boolean;
  [key: string]: unknown;
}

export interface NotificationDeps {
  profiles: UserProfileStore;
  templates: TemplateRenderer;
  getConnection: () => MailConnection;
  settings: NotificationSettings;
}

export async function sendNotification(
  deps: NotificationDeps,
  recipientUsers: AdminUser[],
  notification: string,
  extraContext: Record<string, unknown>,
): Promise<boolean> {
  // Get list of email addresses
  const emailRecipients = recipientUsers.filter(
    (recipient) => recipient.isActive && recipient.email && deps.profiles.isEnabled(recipient, notification + '_notifications'),
  );

  // Return if there are no email addresses
  if (emailRecipients.length === 0) {
    return true;
  }

  // Get template
  const templateSubject = 'admin/notifications/' + notification + '_subject.txt';
  const templateText = 'admin/notifications/' + notification + '.txt';
  const templateHtml = 'admin/notifications/' + notification + '.html';

  // Common context to template
  const context: Record<string, unknown> = { settings: deps.settings, ...extraContext };

  const connection = deps.getConnection();
  await connection.open();
  let sentCount = 0;
  try {
    for (const recipient of emailRecipients) {
      // update context with this recipient
      context.user = recipient;

      // Render in the recipient's preferred language
      const language = recipient.preferredLanguage;
      const emailSubject = deps.templates.render(templateSubject, context, language).trim();
      const emailContent = deps.templates.render(templateText, context, language).trim();

      const options: { htmlMessage?: string } = {};
      if (deps.settings.useHtml) {
        options.htmlMessage = deps.templates.render(templateHtml, context, language);
      }

      try {
        await connection.sendMail(emailSubject, emailContent, [recipient.email], options);
        sentCount += 1;
      } catch (err) {
        logger.error(
          "Failed to send notification email '{}' to {}",
          emailSubject,
          recipient.email,
          err instanceof Error ? err.stack : String(err),
        );
      }
    }
  } finally {
    await connection.close();
  }

  return sentCount === emailRecipients.length;
}
