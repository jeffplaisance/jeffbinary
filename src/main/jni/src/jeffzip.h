// Copyright 2010 Jeff Plaisance
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.

#include <stdint.h>

int32_t jeffzip_compress(uint8_t* dataPtr, int32_t dataLength, uint8_t* outPtr);

int32_t jeffzip_decompress(uint8_t* dataPtr, uint8_t* outPtr);