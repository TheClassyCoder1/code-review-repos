module DocIntake
  module Part3
    # Bounds how long a background worker keeps going in one invocation.
    # Workers call #spend! once per cycle; when either the cycle count or the
    # wall-clock deadline is used up it raises StopIteration, which ends the
    # enclosing Kernel#loop cleanly so the scheduler can start a fresh run.
    class RetryBudget
      attr_reader :spent

      def initialize(max_cycles:, deadline_seconds:, clock: -> { Process.clock_gettime(Process::CLOCK_MONOTONIC) })
        raise ArgumentError, "max_cycles must be positive" unless max_cycles.to_i.positive?
        raise ArgumentError, "deadline_seconds must be positive" unless deadline_seconds.to_f.positive?

        @max_cycles = max_cycles
        @clock = clock
        @deadline = clock.call + deadline_seconds
        @spent = 0
      end

      def self.for_worker(source = ENV)
        new(
          max_cycles: Integer(source.fetch("INTAKE_WORKER_MAX_CYCLES", "500")),
          deadline_seconds: Float(source.fetch("INTAKE_WORKER_DEADLINE", "240"))
        )
      end

      def spend!
        raise StopIteration, "cycle budget of #{@max_cycles} used" if @spent >= @max_cycles
        raise StopIteration, "worker deadline reached" if @clock.call >= @deadline

        @spent += 1
      end

      def exhausted?
        @spent >= @max_cycles || @clock.call >= @deadline
      end
    end
  end
end
