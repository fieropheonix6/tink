# Copyright 2021 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS-IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# [START deterministic-aead-example]
"""A command-line utility for encrypting small files with Determinsitic AEAD.

It loads cleartext keys from disk - this is not recommended!
"""

from absl import app
from absl import flags
from absl import logging

import tink
from tink import cleartext_keyset_handle
from tink import daead


FLAGS = flags.FLAGS

flags.DEFINE_enum('mode', None, ['encrypt', 'decrypt'],
                  'The operation to perform.')
flags.DEFINE_string('keyset_path', None,
                    'Path to the keyset used for encryption.')
flags.DEFINE_string('input_path', None, 'Path to the input file.')
flags.DEFINE_string('output_path', None, 'Path to the output file.')
flags.DEFINE_string('associated_data', None,
                    'Optional associated data used for the encryption.')


def main(argv):
  del argv  # Unused.

  associated_data = b'' if not FLAGS.associated_data else bytes(
      FLAGS.associated_data, 'utf-8')

  # Initialise Tink
  daead.register()

  # Read the keyset into a keyset_handle
  with open(FLAGS.keyset_path, 'rt') as keyset_file:
    try:
      text = keyset_file.read()
      keyset_handle = cleartext_keyset_handle.read(tink.JsonKeysetReader(text))
    except tink.TinkError as e:
      logging.exception('Error reading key: %s', e)
      return 1

  # Get the primitive
  try:
    cipher = keyset_handle.primitive(daead.DeterministicAead)
  except tink.TinkError as e:
    logging.error('Error creating primitive: %s', e)
    return 1

  with open(FLAGS.input_path, 'rb') as input_file:
    input_data = input_file.read()
    if FLAGS.mode == 'decrypt':
      output_data = cipher.decrypt_deterministically(
          input_data, associated_data)
    elif FLAGS.mode == 'encrypt':
      output_data = cipher.encrypt_deterministically(
          input_data, associated_data)
    else:
      logging.error(
          'Error mode not supported. Please choose "encrypt" or "decrypt".')
      return 1

    with open(FLAGS.output_path, 'wb') as output_file:
      output_file.write(output_data)


if __name__ == '__main__':
  flags.mark_flags_as_required([
      'mode', 'keyset_path', 'input_path', 'output_path'])
  app.run(main)
# [END deterministic-aead-example]