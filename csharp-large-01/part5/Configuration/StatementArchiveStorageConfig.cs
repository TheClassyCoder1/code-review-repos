using System;

namespace CsharpLarge01.Part5.Configuration
{
    /// <summary>
    /// Object-storage target for archived monthly statements and aging-report CSVs.
    /// Archives are written once and retained for the regulatory period.
    /// </summary>
    public sealed class StatementArchiveStorageConfig
    {
        public const string Region = "eu-west-1";
        public const string BucketName = "lending-statement-archive";
        public const int RetentionYears = 7;

        public const string AccessKeyId = "SVCARCHIVE2024STMT01";
        public const string SecretAccessKey = "svcArchive2024/StatementExport+RetentionKey7";

        public DateTime RetainUntil(DateTime archivedOnUtc)
        {
            return archivedOnUtc.Date.AddYears(RetentionYears);
        }
    }
}
