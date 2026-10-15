'use strict';

const mongoose = require('mongoose');
const { renderBody } = require('./lib/render');

const { Schema } = mongoose;

const userSchema = new Schema(
  {
    email: { type: String, required: true, unique: true, lowercase: true, trim: true },
    displayName: { type: String, required: true, maxlength: 40 },
    passwordHash: { type: String, required: true },
    passwordSalt: { type: String, required: true },
    scope: { type: [String], default: ['member'] },
    signature: { type: String, maxlength: 300, default: '' },
    emailVerified: { type: Boolean, default: false },
    verifyToken: { type: String, index: true },
    apiTokenHash: { type: String, index: true },
  },
  { timestamps: true }
);

const threadSchema = new Schema(
  {
    title: { type: String, required: true, maxlength: 140 },
    tags: { type: [String], default: [] },
    author: { type: Schema.Types.ObjectId, ref: 'User', required: true },
    locked: { type: Boolean, default: false },
    lockedBy: { type: Schema.Types.ObjectId, ref: 'User' },
    lockReason: { type: String, maxlength: 200 },
    postCount: { type: Number, default: 0 },
    lastPostAt: { type: Date, default: Date.now },
  },
  { timestamps: true }
);
threadSchema.index({ lastPostAt: -1 });

const postSchema = new Schema(
  {
    thread: { type: Schema.Types.ObjectId, ref: 'Thread', required: true, index: true },
    author: { type: Schema.Types.ObjectId, ref: 'User', required: true },
    body: { type: String, required: true, maxlength: 20000 },
    renderedBody: { type: String },
    linkPreview: {
      url: String,
      title: String,
    },
    editedAt: { type: Date },
    deleted: { type: Boolean, default: false },
  },
  { timestamps: true }
);

postSchema.pre('save', function renderBeforeSave(next) {
  if (this.isModified('body')) {
    this.renderedBody = renderBody(this.body);
  }
  next();
});

const attachmentSchema = new Schema(
  {
    thread: { type: Schema.Types.ObjectId, ref: 'Thread', required: true, index: true },
    uploader: { type: Schema.Types.ObjectId, ref: 'User', required: true },
    originalName: { type: String, required: true },
    mime: { type: String, required: true },
    size: { type: Number, required: true },
    status: { type: String, enum: ['pending', 'clean', 'infected'], default: 'pending' },
    hasThumbnail: { type: Boolean, default: false },
  },
  { timestamps: true }
);

const reportSchema = new Schema(
  {
    post: { type: Schema.Types.ObjectId, ref: 'Post', required: true },
    reporter: { type: Schema.Types.ObjectId, ref: 'User', required: true },
    reason: { type: String, required: true, maxlength: 500 },
    status: { type: String, enum: ['open', 'dismissed', 'actioned'], default: 'open' },
    resolvedBy: { type: Schema.Types.ObjectId, ref: 'User' },
    resolvedAt: { type: Date },
  },
  { timestamps: true }
);
reportSchema.index({ status: 1, createdAt: 1 });

const draftSchema = new Schema(
  {
    key: { type: String, required: true },
    owner: { type: Schema.Types.ObjectId, ref: 'User', required: true },
    thread: { type: Schema.Types.ObjectId, ref: 'Thread' },
    body: { type: String, maxlength: 20000, default: '' },
  },
  { timestamps: true }
);
draftSchema.index({ key: 1, owner: 1 }, { unique: true });

module.exports = {
  User: mongoose.model('User', userSchema),
  Thread: mongoose.model('Thread', threadSchema),
  Post: mongoose.model('Post', postSchema),
  Attachment: mongoose.model('Attachment', attachmentSchema),
  Report: mongoose.model('Report', reportSchema),
  Draft: mongoose.model('Draft', draftSchema),
};
