import { HttpService } from '@nestjs/axios';
import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { firstValueFrom } from 'rxjs';
import { PharmacyOrder } from '../entities/pharmacy-order.entity';
import { PrescriptionItem } from '../entities/prescription.entity';

export class PharmacyApiError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}

export interface SubmitResult {
  externalRef: string;
  estimatedReadyAt: string | null;
}

export interface StockLevel {
  ndc: string;
  available: number;
}

export type RemoteOrderStatus = 'received' | 'filling' | 'ready' | 'shipped' | 'delivered' | 'cancelled';

@Injectable()
export class PharmacyClient {
  private readonly baseUrl: string;
  private readonly apiKey: string;
  private readonly timeoutMs: number;

  constructor(
    private readonly http: HttpService,
    config: ConfigService,
  ) {
    this.baseUrl = config.get<string>('pharmacy.baseUrl');
    this.apiKey = config.get<string>('pharmacy.apiKey');
    this.timeoutMs = config.get<number>('pharmacy.timeoutMs');
  }

  private headers(): Record<string, string> {
    return { authorization: `Bearer ${this.apiKey}`, 'content-type': 'application/json' };
  }

  async submitOrder(order: PharmacyOrder, items: PrescriptionItem[]): Promise<SubmitResult> {
    const res = await fetch(`${this.baseUrl}/v2/orders`, {
      method: 'POST',
      headers: this.headers(),
      body: JSON.stringify({
        client_reference: order.id,
        store_id: order.storeId,
        delivery_address: order.deliveryAddress,
        lines: items.map((item) => ({ ndc: item.ndc, quantity: item.quantity, sig: item.dosage })),
      }),
    });
    if (!res.ok) {
      throw new PharmacyApiError(res.status, `Order submission failed with status ${res.status}`);
    }
    const body = (await res.json()) as { order_id: string; estimated_ready_at?: string };
    return { externalRef: body.order_id, estimatedReadyAt: body.estimated_ready_at ?? null };
  }

  async cancelOrder(externalRef: string): Promise<void> {
    const res = await fetch(`${this.baseUrl}/v2/orders/${encodeURIComponent(externalRef)}/cancel`, {
      method: 'POST',
      headers: this.headers(),
      signal: AbortSignal.timeout(this.timeoutMs),
    });
    if (!res.ok && res.status !== 409) {
      throw new PharmacyApiError(res.status, `Order cancel failed with status ${res.status}`);
    }
  }

  async checkStock(storeId: string, ndcs: string[]): Promise<StockLevel[]> {
    const { data } = await firstValueFrom(
      this.http.get<{ stock: StockLevel[] }>(`/v2/stores/${encodeURIComponent(storeId)}/stock`, {
        params: { ndc: ndcs.join(',') },
        headers: this.headers(),
      }),
    );
    return data.stock;
  }

  private ensureReference(externalRef: string | null): string {
    if (!externalRef) {
      throw new PharmacyApiError(400, 'Order has not been transmitted');
    }
    return externalRef;
  }

  async getOrderStatus(externalRef: string): Promise<RemoteOrderStatus> {
    const ref = this.ensureReference(externalRef);
    const res = await fetch(`${this.baseUrl}/v2/orders/${encodeURIComponent(ref)}`, {
      headers: this.headers(),
      signal: AbortSignal.timeout(this.timeoutMs),
    });
    if (!res.ok) {
      throw new PharmacyApiError(res.status, `Order status lookup failed with status ${res.status}`);
    }
    const body = (await res.json()) as { status: RemoteOrderStatus };
    return body.status;
  }
}
