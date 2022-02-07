// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package subtle

import (
	"fmt"

	"github.com/google/tink/go/aead/internal"
	"github.com/google/tink/go/subtle/random"
	"github.com/google/tink/go/tink"
)

const (
	// AESGCMIVSize is the acceptable IV size defined by RFC 5116.
	AESGCMIVSize = internal.AesGcmIvSize
	// AESGCMTagSize is the acceptable tag size defined by RFC 5116.
	AESGCMTagSize = internal.AesGcmTagSize
)

// AESGCM is an implementation of AEAD interface.
type AESGCM struct {
	insecureIvAesGcm *internal.InsecureIvAesGcm
}

// Assert that AESGCM implements the AEAD interface.
var _ tink.AEAD = (*AESGCM)(nil)

// NewAESGCM returns an AESGCM instance, where key is the AES key with length
// 16 bytes (AES-128) or 32 bytes (AES-256).
func NewAESGCM(key []byte) (*AESGCM, error) {
	insecureIvAesGcm, err := internal.NewInsecureIvAesGcm(key, true /*=prependIv*/)
	return &AESGCM{insecureIvAesGcm}, err
}

// Encrypt encrypts plaintext with associatedData. The returned ciphertext
// contains both the IV used for encryption and the actual ciphertext.
//
// Note: The crypto library's AES-GCM implementation always returns the
// ciphertext with an AESGCMTagSize (16-byte) tag.
func (a *AESGCM) Encrypt(plaintext, associatedData []byte) ([]byte, error) {
	iv := random.GetRandomBytes(AESGCMIVSize)
	return a.insecureIvAesGcm.Encrypt(iv, plaintext, associatedData)
}

// Decrypt decrypts ciphertext with associatedData.
func (a *AESGCM) Decrypt(ciphertext, associatedData []byte) ([]byte, error) {
	if len(ciphertext) < AESGCMIVSize {
		return nil, fmt.Errorf("ciphertext with size %d is too short", len(ciphertext))
	}
	iv := ciphertext[:AESGCMIVSize]
	return a.insecureIvAesGcm.Decrypt(iv, ciphertext, associatedData)
}

// Key returns the AES key.
func (a *AESGCM) Key() []byte {
	return a.insecureIvAesGcm.Key
}