import axios from 'axios';
import { config } from './config';

export const httpClient = axios.create({
  timeout: config.http.timeoutMs,
  headers: { 'user-agent': 'subscription-billing/0.1' },
});
