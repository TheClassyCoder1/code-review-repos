<?php

namespace App\Enums;

enum LeaseTemplate: string
{
    case Standard = 'standard';
    case MonthToMonth = 'month-to-month';
    case Commercial = 'commercial';
    case StudentHousing = 'student-housing';
}
