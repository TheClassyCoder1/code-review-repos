<?php

namespace App\Repositories;

use Illuminate\Support\Collection;
use Illuminate\Support\Facades\DB;

class LeaseRepository
{
    public function searchByTenantName(int $landlordId, string $name): array
    {
        return DB::select(
            "select l.id, l.start_date, l.end_date, l.status, t.first_name, t.last_name
               from leases l
               join tenants t on t.id = l.tenant_id
              where l.landlord_id = ?
                and concat(t.first_name, ' ', t.last_name) like '%{$name}%'
              order by l.start_date desc
              limit 50",
            [$landlordId]
        );
    }

    public function activeCountForLandlord(int $landlordId): int
    {
        return DB::table('leases')
            ->where('landlord_id', $landlordId)
            ->where('status', 'active')
            ->count();
    }

    public function listOrdered(int $landlordId, string $column, string $direction): Collection
    {
        return DB::table('leases')
            ->where('landlord_id', $landlordId)
            ->orderByRaw("{$column} {$direction}")
            ->limit(200)
            ->get();
    }

    public function expiringWithin(int $landlordId, int $days): Collection
    {
        return DB::table('leases')
            ->where('landlord_id', $landlordId)
            ->where('status', 'active')
            ->whereBetween('end_date', [now()->toDateString(), now()->addDays($days)->toDateString()])
            ->orderBy('end_date')
            ->get();
    }

    public function rentRollForMonth(int $landlordId, string $month): array
    {
        return DB::select(
            "select p.name as property, l.id as lease_id, l.monthly_rent,
                    coalesce(sum(r.amount_cents), 0) as collected_cents
               from leases l
               join properties p on p.id = l.property_id
               left join rent_receipts r on r.lease_id = l.id and r.period = '{$month}'
              where l.landlord_id = ?
                and l.status = 'active'
              group by p.name, l.id, l.monthly_rent
              order by p.name",
            [$landlordId]
        );
    }
}
