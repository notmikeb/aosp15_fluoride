# A2DP Java 架構與流程圖

## 一、整體架構圖

```mermaid
graph TB
    subgraph "Android Framework"
        A[BluetoothA2dp API]
        B[BluetoothA2dpSink API]
    end
    
    subgraph "Bluetooth App"
        C[A2dpService]
        D[A2dpSinkService]
        E[A2dpStateMachine]
        F[A2dpSinkStateMachine]
        G[A2dpNativeInterface]
        H[A2dpSinkNativeInterface]
        I[A2dpCodecConfig]
        J[A2dpSinkStreamHandler]
    end
    
    subgraph "Native Stack"
        K[BTIF A2DP]
        L[BTA AV]
        M[AVDT]
    end
    
    subgraph "External Interfaces"
        N[AudioManager]
        O[DatabaseManager]
        P[ActiveDeviceManager]
        Q[AdapterService]
    end
    
    A --> C
    B --> D
    C --> E
    D --> F
    C --> G
    D --> H
    G --> K
    H --> K
    K --> L
    L --> M
    
    C --> N
    C --> O
    C --> P
    C --> Q
    D --> N
    D --> O
    D --> Q
    
    C --> I
    D --> J
```

## 二、主要組件說明

### 1. **A2dpService** (Source 端)
- **功能**：A2DP Source 服務，負責音訊輸出到藍牙設備
- **主要職責**：
  - 管理 A2DP 連接狀態
  - 處理音訊串流控制
  - 管理 Codec 配置
  - 與 Audio Framework 互動

### 2. **A2dpSinkService** (Sink 端)
- **功能**：A2DP Sink 服務，負責接收藍牙設備的音訊
- **主要職責**：
  - 管理 A2DP Sink 連接狀態
  - 處理音訊接收
  - 與 Audio Framework 互動

### 3. **A2dpStateMachine** / **A2dpSinkStateMachine**
- **功能**：狀態機，管理每個設備的連接狀態
- **狀態**：Disconnected → Connecting → Connected → Disconnecting

### 4. **A2dpNativeInterface** / **A2dpSinkNativeInterface**
- **功能**：JNI 介面，與 Native Stack 溝通
- **主要方法**：connectA2dp(), disconnectA2dp(), setActiveDevice()

### 5. **A2dpCodecConfig**
- **功能**：管理 A2DP Codec 配置
- **支援的 Codec**：SBC, AAC, APTX, APTX-HD, LDAC, OPUS

## 三、初始化流程圖

```mermaid
sequenceDiagram
    participant AS as AdapterService
    participant A2S as A2dpService
    participant ANI as A2dpNativeInterface
    participant ACC as A2dpCodecConfig
    participant AM as AudioManager
    participant Native as Native Stack
    
    AS->>A2S: 創建 A2dpService
    A2S->>A2S: 初始化成員變數
    A2S->>ACC: 創建 A2dpCodecConfig
    ACC->>ACC: 載入 Codec 優先級配置
    ACC->>AM: 獲取硬體 Offload 支援
    A2S->>ANI: 初始化 Native Interface
    ANI->>Native: initNative()
    Native-->>ANI: 初始化完成
    A2S->>AM: 註冊 AudioDeviceCallback
    A2S->>A2S: 設置為全域服務實例
```

## 四、連接流程圖

```mermaid
sequenceDiagram
    participant App as Application
    participant A2S as A2dpService
    participant ASM as A2dpStateMachine
    participant ANI as A2dpNativeInterface
    participant Native as Native Stack
    participant Device as Bluetooth Device
    
    App->>A2S: connect(device)
    A2S->>A2S: 檢查連接權限
    A2S->>A2S: 檢查最大連接數
    A2S->>ASM: 獲取或創建 StateMachine
    ASM->>ASM: 發送 MESSAGE_CONNECT
    ASM->>ASM: 切換到 Connecting 狀態
    ASM->>ANI: connectA2dp(device)
    ANI->>Native: connectA2dpNative()
    Native->>Device: 發送連接請求
    Device-->>Native: 連接回應
    Native-->>ANI: onConnectionStateChanged()
    ANI-->>ASM: 發送 MESSAGE_STACK_EVENT
    ASM->>ASM: 切換到 Connected 狀態
    ASM-->>A2S: 廣播連接狀態變更
```

## 五、斷開連接流程圖

```mermaid
sequenceDiagram
    participant App as Application
    participant A2S as A2dpService
    participant ASM as A2dpStateMachine
    participant ANI as A2dpNativeInterface
    participant Native as Native Stack
    participant Device as Bluetooth Device
    
    App->>A2S: disconnect(device)
    A2S->>ASM: 獲取 StateMachine
    ASM->>ASM: 發送 MESSAGE_DISCONNECT
    ASM->>ASM: 切換到 Disconnecting 狀態
    ASM->>ANI: disconnectA2dp(device)
    ANI->>Native: disconnectA2dpNative()
    Native->>Device: 發送斷開請求
    Device-->>Native: 斷開回應
    Native-->>ANI: onConnectionStateChanged()
    ANI-->>ASM: 發送 MESSAGE_STACK_EVENT
    ASM->>ASM: 切換到 Disconnected 狀態
    ASM-->>A2S: 廣播連接狀態變更
```

## 六、A2DP Source 狀態機

```mermaid
stateDiagram-v2
    [*] --> Disconnected
    
    Disconnected --> Connecting : MESSAGE_CONNECT
    Connecting --> Connected : CONNECTION_EVENT (SUCCESS)
    Connecting --> Disconnecting : MESSAGE_DISCONNECT
    Connecting --> Disconnected : CONNECTION_EVENT (FAILED)
    Connecting --> Disconnected : MESSAGE_CONNECT_TIMEOUT
    
    Connected --> Disconnecting : MESSAGE_DISCONNECT
    Connected --> Disconnected : DISCONNECTION_EVENT
    
    Disconnecting --> Disconnected : DISCONNECTION_EVENT
    Disconnecting --> Connecting : MESSAGE_CONNECT
    
    Disconnected --> [*] : doQuit()
```

### 狀態說明

#### **Disconnected 狀態**
- **進入動作**：
  - 設置連接狀態為 `STATE_DISCONNECTED`
  - 移除延遲的斷開訊息
  - 廣播連接狀態變更
  - 如果正在播放，停止播放並廣播音訊狀態
- **處理訊息**：
  - `MESSAGE_CONNECT` → 切換到 Connecting 狀態
  - `MESSAGE_DISCONNECT` → 忽略（已在斷開狀態）

#### **Connecting 狀態**
- **進入動作**：
  - 設置連接狀態為 `STATE_CONNECTING`
  - 廣播連接狀態變更
  - 發送連接請求到 Native Stack
  - 設置連接超時計時器
- **處理訊息**：
  - `CONNECTION_EVENT (SUCCESS)` → 切換到 Connected 狀態
  - `CONNECTION_EVENT (FAILED)` → 切換到 Disconnected 狀態
  - `MESSAGE_CONNECT_TIMEOUT` → 切換到 Disconnected 狀態
  - `MESSAGE_DISCONNECT` → 切換到 Disconnecting 狀態

#### **Connected 狀態**
- **進入動作**：
  - 設置連接狀態為 `STATE_CONNECTED`
  - 廣播連接狀態變更
  - 記錄連接成功
- **處理訊息**：
  - `MESSAGE_DISCONNECT` → 切換到 Disconnecting 狀態
  - `DISCONNECTION_EVENT` → 切換到 Disconnected 狀態
  - `AUDIO_STATE_EVENT` → 處理音訊狀態變更
  - `CODEC_CONFIG_EVENT` → 處理 Codec 配置變更

#### **Disconnecting 狀態**
- **進入動作**：
  - 設置連接狀態為 `STATE_DISCONNECTING`
  - 廣播連接狀態變更
  - 發送斷開請求到 Native Stack
- **處理訊息**：
  - `DISCONNECTION_EVENT` → 切換到 Disconnected 狀態
  - `MESSAGE_CONNECT` → 切換到 Connecting 狀態

## 七、A2DP Sink 狀態機

```mermaid
stateDiagram-v2
    [*] --> Disconnected
    
    Disconnected --> Connecting : MESSAGE_CONNECT
    Connecting --> Connected : CONNECTION_EVENT (SUCCESS)
    Connecting --> Disconnected : CONNECTION_EVENT (FAILED)
    Connecting --> Disconnected : MESSAGE_CONNECT_TIMEOUT
    
    Connected --> Disconnecting : MESSAGE_DISCONNECT
    Connected --> Disconnected : DISCONNECTION_EVENT
    
    Disconnecting --> Disconnected : DISCONNECTION_EVENT
    
    Disconnected --> [*] : doQuit()
```

### 狀態說明

#### **Disconnected 狀態**
- **進入動作**：
  - 設置連接狀態為 `STATE_DISCONNECTED`
  - 廣播連接狀態變更
- **處理訊息**：
  - `MESSAGE_CONNECT` → 切換到 Connecting 狀態
  - `INCOMING_CONNECTION` → 切換到 Connecting 狀態

#### **Connecting 狀態**
- **進入動作**：
  - 設置連接狀態為 `STATE_CONNECTING`
  - 廣播連接狀態變更
  - 設置連接超時計時器
- **處理訊息**：
  - `CONNECTION_EVENT (SUCCESS)` → 切換到 Connected 狀態
  - `CONNECTION_EVENT (FAILED)` → 切換到 Disconnected 狀態
  - `MESSAGE_CONNECT_TIMEOUT` → 切換到 Disconnected 狀態

#### **Connected 狀態**
- **進入動作**：
  - 設置連接狀態為 `STATE_CONNECTED`
  - 廣播連接狀態變更
- **處理訊息**：
  - `MESSAGE_DISCONNECT` → 切換到 Disconnecting 狀態
  - `DISCONNECTION_EVENT` → 切換到 Disconnected 狀態
  - `AUDIO_CONFIG_EVENT` → 處理音訊配置變更

#### **Disconnecting 狀態**
- **進入動作**：
  - 設置連接狀態為 `STATE_DISCONNECTING`
  - 廣播連接狀態變更
- **處理訊息**：
  - `DISCONNECTION_EVENT` → 切換到 Disconnected 狀態

## 八、外部介面說明

### 1. **AudioManager**
- **用途**：音訊設備管理和音訊路由
- **主要互動**：
  - 註冊 `AudioDeviceCallback` 監聽音訊設備變更
  - 獲取硬體 Offload 支援的 Codec
  - 音訊設備切換和路由

### 2. **DatabaseManager**
- **用途**：藍牙設備資料庫管理
- **主要互動**：
  - 儲存/讀取設備連接策略
  - 管理設備配對資訊
  - 儲存 Codec 偏好設定

### 3. **ActiveDeviceManager**
- **用途**：管理活躍設備
- **主要互動**：
  - 設置/移除 A2DP 活躍設備
  - 協調多個音訊 Profile 的活躍設備

### 4. **AdapterService**
- **用途**：藍牙適配器服務
- **主要互動**：
  - 獲取最大連接音訊設備數
  - 檢查 A2DP Offload 支援
  - 設備配對狀態管理

## 九、Codec 配置流程

```mermaid
sequenceDiagram
    participant App as Application
    participant A2S as A2dpService
    participant ACC as A2dpCodecConfig
    participant ANI as A2dpNativeInterface
    participant Native as Native Stack
    participant Device as Bluetooth Device
    
    App->>A2S: setCodecConfigPreference()
    A2S->>ACC: setCodecConfigPreference()
    ACC->>ACC: 驗證 Codec 配置
    ACC->>ANI: setCodecConfigPreference()
    ANI->>Native: setCodecConfigPreferenceNative()
    Native->>Device: 發送 Codec 配置
    Device-->>Native: Codec 配置回應
    Native-->>ANI: onCodecConfigChanged()
    ANI-->>A2S: 發送 Codec 配置事件
    A2S->>A2S: 廣播 Codec 狀態變更
```

## 十、音訊串流控制

### **A2DP Source 音訊控制**
```mermaid
sequenceDiagram
    participant Audio as Audio Framework
    participant A2S as A2dpService
    participant ASM as A2dpStateMachine
    participant ANI as A2dpNativeInterface
    participant Native as Native Stack
    
    Audio->>A2S: 音訊串流開始
    A2S->>ASM: 檢查連接狀態
    ASM->>ANI: 確認音訊串流
    ANI->>Native: 音訊串流控制
    Native-->>ANI: 串流狀態回應
    ANI-->>ASM: 音訊狀態事件
    ASM->>ASM: 更新播放狀態
    ASM-->>A2S: 廣播音訊狀態變更
```

### **A2DP Sink 音訊控制**
```mermaid
sequenceDiagram
    participant Device as Bluetooth Device
    participant Native as Native Stack
    participant ANI as A2dpSinkNativeInterface
    participant ASM as A2dpSinkStateMachine
    participant A2SS as A2dpSinkService
    participant Audio as Audio Framework
    
    Device->>Native: 音訊資料
    Native-->>ANI: 音訊資料事件
    ANI-->>ASM: 音訊配置事件
    ASM->>A2SS: 更新音訊配置
    A2SS->>Audio: 音訊資料輸出
```

## 十一、總結

A2DP Java 層的架構設計遵循以下原則：

1. **分層架構**：API 層 → Service 層 → StateMachine 層 → Native Interface 層
2. **狀態機模式**：每個設備都有獨立的狀態機管理連接狀態
3. **事件驅動**：透過訊息機制處理各種事件
4. **模組化設計**：Codec 配置、音訊處理等功能獨立模組
5. **外部介面整合**：與 Audio Framework、Database、Active Device Manager 等緊密整合

這種設計確保了 A2DP 功能的穩定性、可維護性和擴展性。 