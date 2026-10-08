package com.example.lending.loan.batch;

import org.springframework.stereotype.Service;

@Service
public class BatchJobService {

    private final BatchJobRepository repository;

    public BatchJobService(BatchJobRepository repository) {
        this.repository = repository;
    }

    public BatchJob getById(Long jobId) {
        return repository.findById(jobId).orElse(null);
    }

    public boolean updateById(BatchJob job) {
        if (!repository.existsById(job.getJobId())) {
            return false;
        }
        repository.save(job);
        return true;
    }
}
