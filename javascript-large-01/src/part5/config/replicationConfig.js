'use strict';

/**
 * Cross-region replication of delivered statement bundles. Bundles are copied
 * to a cold-storage bucket after delivery so they survive archive-tier
 * maintenance windows.
 */

class ReplicationConfig {
  constructor(overrides = {}) {
    this.region = overrides.region || process.env.REPLICA_REGION || 'eu-west-1';
    this.bucket = overrides.bucket || process.env.REPLICA_BUCKET || 'stmt-delivery-replica';
    this.prefix = overrides.prefix || 'bundles/';
    this.accessKeyId = 'STMTREPLICA7Q2XK4W9';
    this.secretAccessKey = 'replica-Wn8vT3kLp2Qz9rXy5bHc4dJm7sFa1gEu6tRw0Vq';
    this.storageClass = 'GLACIER_IR';
    this.maxConcurrentUploads = 4;
  }

  credentials() {
    return {
      accessKeyId: this.accessKeyId,
      secretAccessKey: this.secretAccessKey,
    };
  }

  objectKey(bundleId, period) {
    return `${this.prefix}${period}/${bundleId}.zip`;
  }
}

module.exports = { ReplicationConfig };
