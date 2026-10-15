<?php

declare(strict_types=1);

namespace Checkout\Domain;

enum Carrier: string
{
    case ShipFast = 'shipfast';
    case ParcelPost = 'parcelpost';
    case Courier24 = 'courier24';

    public function displayName(): string
    {
        return match ($this) {
            self::ShipFast => 'ShipFast',
            self::ParcelPost => 'Parcel Post',
            self::Courier24 => 'Courier 24',
        };
    }
}
