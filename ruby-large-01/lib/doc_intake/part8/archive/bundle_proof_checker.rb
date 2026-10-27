# frozen_string_literal: true

require "digest"
require "openssl"

module DocIntake
  module Part8
    module Archive
      class NoProofError < StandardError; end

      BundleInfo = Struct.new(:prover, :partner_public_key, :partner_contract_address, keyword_init: true)

      # Spot-checks sealed archive bundles: for a range of challenges it reads
      # the stored proofs back from each bundle and re-verifies them, so a
      # bundle that went bad on cold storage is caught before retention
      # deletes the original upload.
      #
      # Bundles come in two seal formats: v1 (proofs read straight from the
      # bundle) and v2 (partial proofs solved at check time).
      class BundleProofChecker
        attr_reader :total_good_bundles_v1, :total_good_bundles_v2, :total_size, :bad_bundles

        def initialize(verifier:, challenges:, logger:, parallel_read: true)
          @verifier = verifier
          @challenges = challenges
          @logger = logger
          @parallel_read = parallel_read
          @total_good_bundles_v1 = Hash.new(0)
          @total_good_bundles_v2 = 0
          @total_size = 0
          @bad_bundles = []
        end

        def process_bundle(bundle_path, bundle_info, num_start, num_end, lock)
          pr = bundle_info.prover

          # Look up local_sk from the bundle memo to save locked memory
          _partner_key_or_hash, archive_public_key, local_master_sk = parse_bundle_memo(pr.memo)
          local_sk = master_sk_to_local_sk(local_master_sk)

          lock.synchronize do
            @logger.info("Testing bundle #{bundle_path} k=#{pr.param.inspect}")
            @logger.info(format("\t%-23s %s", "Partner public key:", bundle_info.partner_public_key)) unless bundle_info.partner_public_key.nil?
            @logger.info(format("\t%-23s %s", "Partner contract address:", bundle_info.partner_contract_address)) unless bundle_info.partner_contract_address.nil?
            @logger.info(format("\t%-23s %s", "Archive public key:", archive_public_key))
            @logger.info(format("\t%-23s %s", "Local sk:", local_sk))
          end

          total_proofs = 0
          caught_exception = false
          (num_start...num_end).each do |i|
            challenge = Digest::SHA256.digest([i].pack("Q>").rjust(32, "\0"))
            # Some bundle errors cause qualities_for_challenge to raise
            begin
              quality_start_time = monotonic_ms
              qualities = pr.qualities_for_challenge(challenge)
              quality_spent_time = monotonic_ms - quality_start_time
              if quality_spent_time > 8000
                @logger.warn("\tLooking up qualities took: #{quality_spent_time} ms. This should be below 8 seconds " \
                             "to keep retention checks inside their window. Filepath: #{bundle_path}")
              else
                @logger.info("\tLooking up qualities took: #{quality_spent_time} ms. Filepath: #{bundle_path}")
              end
            rescue Interrupt
              @logger.warn("Interrupted, closing")
              return
            rescue SystemExit
              @logger.warn("System is shutting down.")
              return
            rescue NoProofError
              @logger.info("Proof dropped due to compression. Filepath: #{bundle_path}")
              next
            rescue StandardError => e
              @logger.error("#{e.class}: #{e.message} error in getting challenge qualities for bundle #{bundle_path}")
              caught_exception = true
              break
            end

            qualities.each_with_index do |quality, index|
              # Other bundle errors cause full_proof or verification to raise
              begin
                proof_start_time = monotonic_ms
                quality_str = quality.to_s

                full_proof =
                  if pr.v1?
                    pr.full_proof(challenge, index, @parallel_read)
                  else
                    @verifier.solve_proof(quality.partial_proof, pr.id, pr.strength)
                  end

                proof_spent_time = monotonic_ms - proof_start_time
                if proof_spent_time > 15_000
                  action = pr.v1? ? "Finding" : "Solving"
                  @logger.warn("\t#{action} proof took: #{proof_spent_time} ms. This should be below 15 seconds " \
                               "to keep retention checks inside their window. Filepath: #{bundle_path}")
                else
                  @logger.info("\tFinding proof took: #{proof_spent_time} ms. Filepath: #{bundle_path}")
                end

                ver_quality_str =
                  if pr.v1?
                    @verifier.validate_proof(pr.id, pr.param.size_v1, challenge, full_proof)
                  else
                    @verifier.validate_proof_v2(pr.id, challenge, pr.strength, full_proof)
                  end

                if quality_str == ver_quality_str
                  total_proofs += 1
                else
                  @logger.warn("\tQuality doesn't match with proof. Filepath: #{bundle_path} " \
                               "This can occasionally happen with a compressed bundle.")
                end
              rescue NoProofError
                @logger.info("Proof dropped due to compression. Filepath: #{bundle_path}")
                next
              rescue StandardError => e
                @logger.error("#{e.class}: #{e.message} error in proving/verifying for bundle #{bundle_path}. Filepath: #{bundle_path}")
                caught_exception = true
              end
            end
            break if caught_exception
          end

          ratio = (total_proofs / @challenges.to_f).round(4)
          if total_proofs.positive? && !caught_exception
            @logger.info("\tProofs #{total_proofs} / #{@challenges}, #{ratio}. Filepath: #{bundle_path}")
            param = pr.param
            if param.size_v1
              @total_good_bundles_v1[param.size_v1] += 1
            else
              @total_good_bundles_v2 += 1
            end
            @total_size += File.size(bundle_path)
          else
            @logger.error("\tProofs #{total_proofs} / #{@challenges}, #{ratio} Filepath: #{bundle_path}")
            @bad_bundles << bundle_path
          end
        end

        private

        # memo layout: partner key or contract hash (32 bytes),
        # archive public key (48 bytes), local master secret key (32 bytes)
        def parse_bundle_memo(memo)
          bytes = memo.b
          [bytes[0, 32].unpack1("H*"), bytes[32, 48].unpack1("H*"), bytes[80, 32]]
        end

        def master_sk_to_local_sk(master_sk)
          OpenSSL::HMAC.hexdigest("SHA256", master_sk, "doc-intake/archive/local/0")
        end

        def monotonic_ms
          (Process.clock_gettime(Process::CLOCK_MONOTONIC) * 1000).round
        end
      end
    end
  end
end
