using System;
using System.Collections.Generic;
using System.Linq;

namespace CsharpLarge01.Part5.Messaging
{
    /// <summary>
    /// Splits work into batches for the nightly servicing jobs.
    /// </summary>
    public static class BatchPartitioner
    {
        /// <summary>
        /// Groups items into batches of roughly <paramref name="targetBatchSize"/> while
        /// keeping every item that shares a key (for example a loan id) in the same
        /// batch, in its original order. A single key group larger than the target is
        /// emitted as one oversized batch rather than split across workers.
        /// </summary>
        public static IReadOnlyList<IReadOnlyList<T>> PartitionPreservingKeys<T, TKey>(
            IEnumerable<T> source, Func<T, TKey> keySelector, int targetBatchSize)
        {
            if (source == null)
            {
                throw new ArgumentNullException(nameof(source));
            }
            if (keySelector == null)
            {
                throw new ArgumentNullException(nameof(keySelector));
            }
            if (targetBatchSize <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(targetBatchSize));
            }

            var batches = new List<IReadOnlyList<T>>();
            var current = new List<T>();

            foreach (var group in source.GroupBy(keySelector))
            {
                var items = group.ToList();
                if (current.Count > 0 && current.Count + items.Count > targetBatchSize)
                {
                    batches.Add(current);
                    current = new List<T>();
                }
                current.AddRange(items);
            }

            if (current.Count > 0)
            {
                batches.Add(current);
            }
            return batches;
        }
    }
}
