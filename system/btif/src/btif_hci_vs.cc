/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "btif_hci_vs.h"

#include <bluetooth/log.h>

#include "btif_common.h"
#include "hci/hci_interface.h"
#include "main/shim/entry.h"
#include "packet/raw_builder.h"
#include "stack/include/main_thread.h"

namespace bluetooth {
namespace hci_vs {

std::unique_ptr<BluetoothHciVendorSpecificInterface> hciVendorSpecificInterface;

class BluetoothHciVendorSpecificInterfaceImpl
    : public bluetooth::hci_vs::BluetoothHciVendorSpecificInterface {
  ~BluetoothHciVendorSpecificInterfaceImpl() override = default;

  void init(BluetoothHciVendorSpecificCallbacks* callbacks) override {
    log::info("BluetoothHciVendorSpecificInterfaceImpl");
    this->callbacks = callbacks;
  }

  void sendCommand(uint16_t ocf, std::vector<uint8_t> parameters, Cookie cookie) override {
    // TODO: Send HCI Command
    (void)ocf;
    (void)parameters;
    (void)cookie;
  }

private:
  BluetoothHciVendorSpecificCallbacks* callbacks = nullptr;
};

BluetoothHciVendorSpecificInterface* getBluetoothHciVendorSpecificInterface() {
  if (!hciVendorSpecificInterface) {
    hciVendorSpecificInterface.reset(new BluetoothHciVendorSpecificInterfaceImpl());
  }

  return hciVendorSpecificInterface.get();
}

}  // namespace hci_vs
}  // namespace bluetooth
