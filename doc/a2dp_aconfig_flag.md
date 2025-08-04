# A2DP aconfig 功能標誌系統說明

## 一、aconfig 是什麼？

`aconfig` 是 Android 的**功能標誌（Feature Flags）管理系統**，用於：
- **動態控制功能開關**：在編譯時或運行時控制特定功能的啟用/禁用
- **A/B 測試**：為不同設備或用戶群組啟用不同的功能組合
- **Bug 修復控制**：逐步推出修復，避免一次性影響所有用戶
- **性能優化**：控制實驗性功能的啟用

## 二、a2dp.aconfig 檔案結構

```aconfig
package: "com.android.bluetooth.flags"  # 套件名稱
container: "com.android.btservices"     # 容器名稱

flag {
    name: "a2dp_ignore_started_when_responder"  # 標誌名稱
    namespace: "bluetooth"                      # 命名空間
    description: "Ignore the event BTA_AV_START_EVT when source and responder"  # 描述
    bug: "341178856"                            # 相關 Bug ID
    metadata {
        purpose: PURPOSE_BUGFIX                 # 用途（Bug 修復）
    }
}
```

## 三、主要標誌類型

### **Bug 修復標誌**
```aconfig
flag {
    name: "a2dp_source_threading_fix"
    description: "Schedule A2DP source setup operations to bt_main_thread to prevent races"
    bug: "374166531"
    metadata {
        purpose: PURPOSE_BUGFIX
    }
}
```

### **API 變更標誌**
```aconfig
flag {
    name: "a2dp_lhdc_api"
    description: "API change for LHDC codec support"
    is_exported: true  # 可被外部模組使用
    bug: "380118954"
}
```

### **功能控制標誌**
```aconfig
flag {
    name: "a2dp_service_looper"
    description: "Inject looper into A2dpService"
    bug: "337348333"
    metadata {
        purpose: PURPOSE_BUGFIX
    }
}
```

## 四、在程式碼中的使用方式

### **C++ 中使用**
```cpp
// 檢查標誌是否啟用
if (com::android::bluetooth::flags::a2dp_ignore_started_when_responder()) {
    // 執行新邏輯
} else {
    // 執行舊邏輯
}

// 條件性執行
if (com::android::bluetooth::flags::a2dp_source_threading_fix()) {
    return get_main_thread();  // 使用主線程
} else {
    return &btif_a2dp_source_thread;  // 使用專用線程
}

// 音訊串流檢查
if (com::android::bluetooth::flags::a2dp_check_lea_iso_channel() &&
    hci::IsoManager::GetInstance()->GetNumberOfActiveIso() > 0) {
    log::error("unable to start stream: LEA is active");
    return Status::FAILURE;
}
```

### **Java 中使用**
```java
// 檢查標誌
if (Flags.a2dpIgnoreStartedWhenResponder()) {
    // 新行為
} else {
    // 舊行為
}

// 服務初始化
if (!Flags.a2dpServiceLooper()) {
    mStateMachinesThread = new HandlerThread("A2dpService.StateMachines");
    mStateMachinesThread.start();
} else {
    mStateMachinesThread = null;
}
```

## 五、建置系統整合

### **Android.bp 配置**
```bp
aconfig_declarations {
    name: "bluetooth_aconfig_flags",
    package: "com.android.bluetooth.flags",
    srcs: [
        "a2dp.aconfig",
        "hfp.aconfig",
        // ... 其他 aconfig 檔案
    ],
}

# 產生 C++ 函式庫
cc_aconfig_library {
    name: "bluetooth_flags_c_lib",
    aconfig_declarations: "bluetooth_aconfig_flags",
}

# 產生 Java 函式庫
java_aconfig_library {
    name: "bluetooth_flags_java_lib",
    aconfig_declarations: "bluetooth_aconfig_flags",
}
```

### **aconfig.gni 建置模板**
```gn
template("aconfig") {
    # 產生 C++ 檔案
    action("${target_name}_cpp") {
        script = "//common-mk/file_generator_wrapper.py"
        args = [
            "aconfig",
            "create-cpp-lib",
            "--cache=${outdir}/${aconfig_cache}",
            "--out=${outdir}",
        ]
        outputs = [
            "${outdir}/include/${aconfig_cpp_file_name}.h",
            "${outdir}/${aconfig_cpp_file_name}.cc",
        ]
    }
}
```

## 六、實際應用範例

### **A2DP 線程管理修復**
```cpp
// 在 btif_a2dp_source.cc 中
static bluetooth::common::MessageLoopThread* local_thread() {
    return com::android::bluetooth::flags::a2dp_source_threading_fix() 
           ? get_main_thread()           // 新：使用主線程
           : &btif_a2dp_source_thread;   // 舊：使用專用線程
}
```

### **AVDTP 錯誤碼修復**
```cpp
// 在 avdt_scb_act.cc 中
if (com::android::bluetooth::flags::avdtp_error_codes()) {
    // 使用符合標準的錯誤碼
    return AVDTP_UNSUPPORTED_CONFIGURATION;
} else {
    // 使用舊的錯誤碼
    return AVDTP_BAD_STATE;
}
```

### **串流重配置修復**
```cpp
// 在 btif_av.cc 中
if (com::android::bluetooth::flags::av_stream_reconfigure_fix() &&
    peer_.CheckFlags(BtifAvPeer::kFlagPendingReconfigure)) {
    log::info("Peer {} : Stream started but reconfiguration pending. Reconfiguring stream",
              peer_.PeerAddress());
    btif_av_source_dispatch_sm_event(peer_.PeerAddress(), BTIF_AV_RECONFIGURE_REQ_EVT);
}
```

## 七、標誌控制方式

### **編譯時控制**
- 在 `aconfig` 檔案中設定預設值
- 透過建置系統傳遞參數

### **運行時控制**
- 透過系統屬性控制
- 透過開發者選項控制
- 透過遠端配置控制

## 八、主要 A2DP 相關標誌

| 標誌名稱 | 用途 | 類型 |
|---------|------|------|
| `a2dp_service_looper` | 注入 looper 到 A2dpService | Bug 修復 |
| `avdtp_error_codes` | 使用符合標準的錯誤碼 | Bug 修復 |
| `bta_av_use_peer_codec` | 棄用 bta av codec state | Bug 修復 |
| `a2dp_ignore_started_when_responder` | 忽略 responder 的 START 事件 | Bug 修復 |
| `avrcp_sdp_records` | 更新 AVRCP SDP 記錄 | Bug 修復 |
| `a2dp_check_lea_iso_channel` | 檢查 LEA ISO 通道 | Bug 修復 |
| `a2dp_variable_aac_capability` | 啟用 AAC 48kHz 採樣率 | 功能 |
| `stop_on_offload_fail` | Offload 失敗時停止而非斷開 | Bug 修復 |
| `a2dp_aidl_encoding_interval` | 配置音訊 BT HAL 的 PcmConfig 間隔 | Bug 修復 |
| `av_stream_reconfigure_fix` | 在事件中處理 AVDT 串流重配置 | Bug 修復 |
| `a2dp_source_threading_fix` | 將 A2DP source 設置操作排程到主線程 | Bug 修復 |
| `a2dp_clear_pending_start_on_session_restart` | 在會話重啟時清除待處理開始標誌 | Bug 修復 |
| `a2dp_lhdc_api` | LHDC codec 支援的 API 變更 | API 變更 |

## 九、優點

1. **漸進式部署**：可以逐步推出新功能或修復
2. **風險控制**：出現問題時可以快速關閉特定功能
3. **A/B 測試**：為不同用戶群組啟用不同功能
4. **維護性**：集中管理功能開關，易於維護
5. **向後相容**：保持舊邏輯作為備選方案

## 十、總結

`a2dp.aconfig` 是 Android Bluetooth 堆疊中 A2DP 相關功能的標誌管理檔案，透過這個系統可以：
- **動態控制** A2DP 相關功能的啟用/禁用
- **逐步推出** Bug 修復和新功能
- **降低風險**，提高系統穩定性
- **支援實驗性功能**的測試和驗證

這種設計讓 Android Bluetooth 堆疊能夠更靈活地管理功能，特別是在處理複雜的音訊協議和設備相容性問題時。 