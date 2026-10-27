import { getRaw } from '../http/httpClient';

export const helpText = `
Google translate bot
Please format your message like:
\`@-mention "<text_to_translate>" <target-language> <source-language(optional)>\`
Visit [here](https://cloud.google.com/translate/docs/languages) for all languages
`;

export const languageNotFoundText = (which: string): string => `${which} language not found. Visit [here](https://cloud.google.com/translate/docs/languages) for all languages`;

export interface TranslateConfig {
  key: string;
}

export interface Language {
  name: string;
  language: string;
}

export class ConnectionError extends Error {}

export class TranslateError extends Error {}

export function getCodeForLanguage(language: string, allLanguages: Language[]): string {
  const wanted = language.toLowerCase();
  const match = allLanguages.find((l) => l.name.toLowerCase() === wanted || l.language.toLowerCase() === wanted);
  return match ? match.language : '';
}

export async function translate(text: string, key: string, dest: string, src: string): Promise<string> {
  const params = new URLSearchParams({ q: text, target: dest, format: 'text', key });
  if (src !== '') {
    params.set('source', src);
  }
  let response;
  try {
    response = await getRaw('https://translation.googleapis.com/language/translate/v2?' + params.toString());
  } catch (e) {
    throw new ConnectionError(e instanceof Error ? e.message : String(e));
  }
  const parsed = JSON.parse(response.body) as {
    data?: { translations: Array<{ translatedText: string }> };
    error?: { message: string };
  };
  if (response.status !== 200 || !parsed.data) {
    throw new TranslateError(parsed.error?.message ?? `status ${response.status}`);
  }
  return parsed.data.translations[0].translatedText;
}

export async function getTranslateBotResponse(
  messageContent: string | null,
  configFile: TranslateConfig,
  author: string,
  allLanguages: Language[],
): Promise<string> {
  if (messageContent == null) {
    return helpText;
  }
  messageContent = messageContent.trim();
  if (messageContent === 'help' || !messageContent.startsWith('"')) {
    return helpText;
  }
  const cut = messageContent.lastIndexOf('" ');
  if (cut < 0) {
    return helpText;
  }
  const splitText = [messageContent.slice(0, cut), ...messageContent.slice(cut + 2).split(' ')];
  if (splitText.length === 2) {
    // There is no source language
    splitText.push('');
  }
  if (splitText.length !== 3) {
    return helpText;
  }
  let [textToTranslate, targetLanguage, sourceLanguage] = splitText;
  textToTranslate = textToTranslate.slice(1);
  targetLanguage = getCodeForLanguage(targetLanguage, allLanguages);
  if (targetLanguage === '') {
    return languageNotFoundText('Target');
  }
  if (sourceLanguage !== '') {
    sourceLanguage = getCodeForLanguage(sourceLanguage, allLanguages);
    if (sourceLanguage === '') {
      return languageNotFoundText('Source');
    }
  }
  let translatedText: string;
  try {
    translatedText = await translate(textToTranslate, configFile.key, targetLanguage, sourceLanguage);
  } catch (err) {
    if (err instanceof ConnectionError) {
      return `Could not connect to Google Translate. ${err}.`;
    }
    if (err instanceof TranslateError) {
      return `Translate Error. ${err}.`;
    }
    return `Error. ${err}.`;
  }
  return `${translatedText} (from ${author})`;
}
