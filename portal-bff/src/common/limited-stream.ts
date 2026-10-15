import { Transform, TransformCallback } from 'stream';
import { PayloadTooLargeException } from '@nestjs/common';

export function limitStream(maxBytes: number): Transform {
  let seen = 0;
  return new Transform({
    transform(chunk: Buffer, _enc: BufferEncoding, done: TransformCallback) {
      seen += chunk.length;
      if (seen > maxBytes) {
        done(new PayloadTooLargeException(`upload exceeds ${maxBytes} bytes`));
        return;
      }
      done(null, chunk);
    },
  });
}
