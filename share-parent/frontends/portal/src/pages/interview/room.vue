<template>
  <div class="interview-room-container" v-loading="loading">
    <!-- 考情 HUD 状态栏 -->
    <div class="interview-hud">
      <div class="hud-left">
        <el-button link class="back-btn" @click="handleBack">
          &larr; 返回大厅
        </el-button>
        <div class="hud-divider"></div>
        <div class="persona-badge" :class="'style-' + sessionData.interviewerStyle">
          <span class="persona-icon">{{ getPersonaMeta.icon }}</span>
          <span class="persona-name">{{ getPersonaMeta.name }}</span>
        </div>
        <el-tag effect="dark" type="info" size="small" class="dimension-tag">
          {{ currentDimension }}
        </el-tag>
      </div>

      <div class="hud-center">
        <!-- 三大环节进度 HUD -->
        <div class="stage-hud">
          <div class="stage-step" :class="{ active: currentStage >= 1, current: currentStage === 1 }">
            <span class="step-badge">1</span>
            <span class="step-text">自我介绍</span>
          </div>
          <span class="stage-arrow">&rarr;</span>
          <div class="stage-step" :class="{ active: currentStage >= 2, current: currentStage === 2 }">
            <span class="step-badge">2</span>
            <span class="step-text">基础八股</span>
          </div>
          <span class="stage-arrow">&rarr;</span>
          <div class="stage-step" :class="{ active: currentStage >= 3, current: currentStage === 3 }">
            <span class="step-badge">3</span>
            <span class="step-text">项目深挖</span>
          </div>
        </div>

        <!-- 剥洋葱深度等级 HUD -->
        <div class="depth-hud">
          <span class="depth-label">追问深度：</span>
          <div class="depth-step" :class="{ active: currentDepth >= 1 }">
            <span class="dot"></span>
            <span>L1 选型摸底</span>
          </div>
          <span class="arrow">&rarr;</span>
          <div class="depth-step" :class="{ active: currentDepth >= 2 }">
            <span class="dot"></span>
            <span>L2 底层机制</span>
          </div>
          <span class="arrow">&rarr;</span>
          <div class="depth-step" :class="{ active: currentDepth >= 3 }">
            <span class="dot"></span>
            <span>L3 生产抗峰</span>
          </div>
        </div>
      </div>

      <div class="hud-right">
        <div class="hud-timer" :class="{ 'warning-timer': remainingSeconds < 300 }">
          <span class="timer-icon">⏱️</span>
          <span>{{ formatTimer(timerSeconds) }} / 60:00</span>
        </div>
        <div class="turn-progress">
          考题：<b>{{ currentTurnNum }}/{{ sessionData.totalTurns || 20 }}</b>
        </div>
        <!-- 往轮记录抽屉开关 -->
        <el-button
          size="small"
          class="history-drawer-btn"
          @click="historyDrawerVisible = true"
        >
          📋 往轮实录 ({{ answeredTurnsCount }})
        </el-button>
        <el-button
          v-if="sessionData.status === 2"
          type="success"
          size="small"
          class="finish-btn completed"
          @click="goToReport"
        >
          📊 查看终局报告
        </el-button>
        <el-button
          v-else-if="sessionData.status === 3"
          type="info"
          size="small"
          class="finish-btn"
          disabled
        >
          🛑 已终止
        </el-button>
        <el-button
          v-else
          type="danger"
          size="small"
          class="finish-btn"
          :loading="finishing"
          @click="handleFinishInterview"
        >
          交卷并生成报告
        </el-button>
      </div>
    </div>

    <!-- 全真远程视频考场核心双视窗舞台 -->
    <div class="video-conference-stage">
      <div class="conference-grid">
        <!-- 左侧：AI 面试官全真全息视窗 -->
        <div class="stage-card interviewer-stage" :class="'theme-' + (sessionData.interviewerStyle || 'p7_architect')">
          <div class="stage-header">
            <div class="stream-status">
              <span class="live-indicator"></span>
              <span class="stream-title">AI 面试官连线 · {{ getPersonaMeta.name }}</span>
            </div>
            <div class="ai-state-badge" :class="aiActivityState">
              <span v-if="aiActivityState === 'speaking'">🗣️ 考官发音提问中</span>
              <span v-else-if="aiActivityState === 'listening'">👂 专注倾听作答中</span>
              <span v-else>🤔 深度推演与题库检定</span>
            </div>
          </div>

          <!-- AI 面试官动态数字人虚拟形象展示台 -->
          <div class="avatar-display-area">
            <!-- 声波能量光环 -->
            <div class="soundwave-halo" :class="{ active: isInterviewerSpeaking || isRecording }">
              <div class="halo-ring ring-1"></div>
              <div class="halo-ring ring-2"></div>
              <div class="halo-ring ring-3"></div>
            </div>

            <!-- 动态矢量数字人形象 -->
            <div class="digital-human-avatar" :class="[aiActivityState, 'persona-' + (sessionData.interviewerStyle || 'p7_architect')]">
              <svg viewBox="0 0 240 280" class="avatar-svg">
                <defs>
                  <!-- 科技渐变底色 -->
                  <linearGradient id="suitGradP7" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stop-color="#1e293b"/>
                    <stop offset="100%" stop-color="#0f172a"/>
                  </linearGradient>
                  <linearGradient id="cyberVisor" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%" stop-color="#06b6d4" stop-opacity="0.8"/>
                    <stop offset="100%" stop-color="#3b82f6" stop-opacity="0.8"/>
                  </linearGradient>
                  <linearGradient id="skinGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                    <stop offset="0%" stop-color="#fbd38d"/>
                    <stop offset="100%" stop-color="#f6ad55"/>
                  </linearGradient>
                  <filter id="glowEffect" x="-20%" y="-20%" width="140%" height="140%">
                    <feGaussianBlur stdDeviation="3" result="blur" />
                    <feComposite in="SourceGraphic" in2="blur" operator="over"/>
                  </filter>
                </defs>

                <!-- 身体/服装 (职业正装/极客服) -->
                <g class="avatar-body">
                  <path d="M 40 280 L 70 210 L 95 215 L 120 235 L 145 215 L 170 210 L 200 280 Z" fill="url(#suitGradP7)"/>
                  <!-- 领口与衬衫/内衬 -->
                  <polygon points="95,215 120,250 145,215" fill="#f8fafc"/>
                  <!-- 职业领带 / 极客胸章 -->
                  <polygon points="116,225 124,225 122,265 118,265" fill="#3b82f6"/>
                  <!-- 科技肩线光条 -->
                  <line x1="70" y1="210" x2="40" y2="280" stroke="#0ea5e9" stroke-width="2" stroke-dasharray="4 4" class="cyber-line"/>
                  <line x1="170" y1="210" x2="200" y2="280" stroke="#0ea5e9" stroke-width="2" stroke-dasharray="4 4" class="cyber-line"/>
                </g>

                <!-- 头部组件（支持呼吸动画） -->
                <g class="avatar-head-group">
                  <!-- 颈部 -->
                  <rect x="106" y="170" width="28" height="42" rx="4" fill="#ed8936"/>
                  
                  <!-- 脸庞 -->
                  <path d="M 75 110 C 75 60, 165 60, 165 110 C 165 160, 145 185, 120 185 C 95 185, 75 160, 75 110 Z" fill="url(#skinGrad)"/>

                  <!-- 科技发型 -->
                  <path d="M 70 100 C 65 50, 130 35, 170 65 C 175 90, 168 115, 160 115 C 155 75, 85 70, 75 105 Z" fill="#1a202c"/>

                  <!-- 眉毛 -->
                  <path d="M 86 100 Q 98 94 110 98" stroke="#2d3748" stroke-width="3.5" fill="none" stroke-linecap="round"/>
                  <path d="M 130 98 Q 142 94 154 100" stroke="#2d3748" stroke-width="3.5" fill="none" stroke-linecap="round"/>

                  <!-- 眼睛（带自动眨眼动画） -->
                  <g class="avatar-eyes">
                    <!-- 左眼 -->
                    <ellipse cx="98" cy="115" rx="7" ry="5" fill="#1a202c" class="eye-pupil"/>
                    <circle cx="100" cy="113" r="2" fill="#ffffff"/>
                    <!-- 右眼 -->
                    <ellipse cx="142" cy="115" rx="7" ry="5" fill="#1a202c" class="eye-pupil"/>
                    <circle cx="144" cy="113" r="2" fill="#ffffff"/>
                  </g>

                  <!-- 全息智能眼镜 / 架构师 HUD 护目片 -->
                  <g v-if="sessionData.interviewerStyle === 'p7_architect' || !sessionData.interviewerStyle" class="cyber-glasses">
                    <rect x="84" y="105" width="28" height="18" rx="4" fill="url(#cyberVisor)" stroke="#38bdf8" stroke-width="1.5" filter="url(#glowEffect)"/>
                    <rect x="128" y="105" width="28" height="18" rx="4" fill="url(#cyberVisor)" stroke="#38bdf8" stroke-width="1.5" filter="url(#glowEffect)"/>
                    <line x1="112" y1="114" x2="128" y2="114" stroke="#38bdf8" stroke-width="2"/>
                  </g>

                  <!-- 鼻子 -->
                  <path d="M 120 118 L 117 136 L 123 136" stroke="#dd6b20" stroke-width="2" fill="none" stroke-linecap="round"/>

                  <!-- 嘴巴（支持发音说话口型动画） -->
                  <g class="avatar-mouth-group" :class="{ 'speaking-mouth': isInterviewerSpeaking }">
                    <path
                      v-if="!isInterviewerSpeaking"
                      d="M 106 156 Q 120 162 134 156"
                      stroke="#c53030"
                      stroke-width="3"
                      fill="none"
                      stroke-linecap="round"
                    />
                    <path
                      v-else
                      class="dynamic-open-mouth"
                      d="M 105 154 Q 120 172 135 154 Q 120 162 105 154 Z"
                      fill="#9b2c2c"
                      stroke="#c53030"
                      stroke-width="2"
                    />
                  </g>
                </g>

                <!-- 耳麦 / 通信天线 -->
                <g class="headset">
                  <path d="M 72 115 C 65 115, 65 140, 75 145" stroke="#38bdf8" stroke-width="4" fill="none"/>
                  <circle cx="75" cy="142" r="5" fill="#0ea5e9" filter="url(#glowEffect)"/>
                  <path d="M 75 142 Q 88 160 102 160" stroke="#0284c7" stroke-width="2.5" fill="none"/>
                  <circle cx="102" cy="160" r="3" fill="#38bdf8"/>
                </g>
              </svg>
            </div>

            <!-- 面试官专属全息风格标签 -->
            <div class="avatar-floating-badge">
              <span class="badge-icon">{{ getPersonaMeta.icon }}</span>
              <span class="badge-title">{{ getPersonaMeta.name }}</span>
            </div>
          </div>

          <!-- 考官实时发问字幕条（带语音发音控制） -->
          <div class="interviewer-question-banner">
            <div class="banner-top-bar">
              <div class="turn-info">
                <el-tag size="small" effect="dark" type="primary">
                  第 {{ currentTurnNum }} 轮 · {{ getDepthText(currentDepth) }}
                </el-tag>
                <span class="sub-dim">【考核核心】：{{ currentDimension }}</span>
              </div>
              <div class="tts-ctrl">
                <el-tag size="small" effect="plain" class="voice-badge" :type="isXiaoxiaoReady ? 'success' : 'info'">
                  <span class="pulse-dot" v-if="isInterviewerSpeaking"></span>
                  🎙️ {{ ttsVoiceLabel }}
                </el-tag>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  class="speak-ctrl-btn"
                  :class="{ active: isInterviewerSpeaking }"
                  @click="toggleTtsCurrentQuestion"
                >
                  <span v-if="isInterviewerSpeaking">⏹️ 暂停朗读</span>
                  <span v-else>🔊 考官重新朗读</span>
                </el-button>
                <el-button
                  size="small"
                  link
                  type="primary"
                  class="preview-voice-btn"
                  @click="previewXiaoxiaoVoice"
                  title="试听晓晓问候音"
                >
                  👋 试听音色
                </el-button>
              </div>
            </div>
            <div class="question-voice-text">
              “{{ currentTurn?.question || '考官正在梳理考点，请做好准备...' }}”
            </div>
          </div>
        </div>

        <!-- 右侧：候选人全真视频连线视窗 -->
        <div class="stage-card candidate-stage">
          <div class="stage-header">
            <div class="stream-status">
              <span class="live-indicator candidate" :class="{ recording: isRecording }"></span>
              <span class="stream-title">候选人（我）· {{ isRecording ? '实时收音作答中' : '连线正常' }}</span>
            </div>
            <div class="device-status-tags">
              <el-tag size="small" :type="cameraActive ? 'success' : 'danger'" effect="plain">
                📷 {{ cameraActive ? '摄像头开启' : '摄像头已关' }}
              </el-tag>
              <el-tag size="small" :type="micActive ? 'success' : 'danger'" effect="plain">
                🎙️ {{ micActive ? '麦克风就绪' : '麦克风静音' }}
              </el-tag>
            </div>
          </div>

          <!-- 候选人真实摄像头画面 / 模拟演示区 -->
          <div class="candidate-video-viewport">
            <video
              ref="candidateVideoRef"
              autoplay
              playsinline
              muted
              class="candidate-video-elem"
              :class="{ 'stream-hidden': !cameraActive }"
            ></video>

            <!-- 摄像头关闭或模拟状态下的学员形象占位 -->
            <div v-if="!cameraActive" class="candidate-simulated-avatar">
              <div class="candidate-silhouette">👨‍💻</div>
              <div class="sim-tip">摄像头已关闭 / 正在以音视频模拟信道参与连线</div>
            </div>

            <!-- 视频内部 HUD 浮层：实时声浪跳动条 (VU-Meter) 与快捷开关 -->
            <div class="candidate-video-hud">
              <!-- 动态音量跳动声波 -->
              <div class="candidate-audio-wave">
                <span class="wave-tag">🎙️ 音压：</span>
                <div class="wave-bars">
                  <div
                    v-for="i in 8"
                    :key="i"
                    class="wave-bar"
                    :style="{ height: getDynamicBarHeight(i) + 'px' }"
                  ></div>
                </div>
              </div>

              <!-- 设备硬件快速切换 -->
              <div class="candidate-device-toggles">
                <el-button
                  circle
                  size="small"
                  :type="cameraActive ? 'primary' : 'info'"
                  :title="cameraActive ? '关闭摄像头' : '打开摄像头'"
                  @click="toggleCamera"
                >
                  📷
                </el-button>
                <el-button
                  circle
                  size="small"
                  :type="micActive ? 'success' : 'danger'"
                  :title="micActive ? '静音麦克风' : '开启麦克风'"
                  @click="toggleMic"
                >
                  🎙️
                </el-button>
              </div>
            </div>
          </div>

          <!-- 答题模式切换栏：口述音视频问答 VS 算法手撕代码实战 -->
          <div class="answering-mode-bar">
            <div class="mode-toggles">
              <el-radio-group v-model="answerMode" size="small">
                <el-radio-button label="voice">🎙️ 口述语音问答</el-radio-button>
                <el-radio-button label="code">💻 手撕代码实战 (沙箱评测)</el-radio-button>
              </el-radio-group>
            </div>
            <div class="mode-hint" v-if="answerMode === 'code'">
              <span class="live-pulse"></span>
              <span>沙箱与考官苏格拉底式启发已就绪</span>
            </div>
          </div>

          <!-- 候选人口述语音实时转写工作台 (取消纯键盘打字，全面采用语音口述) -->
          <div v-if="answerMode === 'voice'" class="candidate-voice-console">
            <div class="console-header">
              <div class="title-with-wave">
                <span class="record-dot" :class="{ pulsing: isRecording }"></span>
                <span class="con-title">{{ isRecording ? '麦克风正在收音并实时转写...' : '候选人口述作答实时转录字幕：' }}</span>
              </div>
              <div class="action-tools">
                <el-button
                  v-if="currentAnswer.trim()"
                  link
                  type="primary"
                  size="small"
                  @click="editDialogVisible = true"
                >
                  ✏️ 微调文字专有名词
                </el-button>
                <span class="char-badge">{{ currentAnswer.length }} 字</span>
              </div>
            </div>

            <!-- 实时语音转录字幕流展区 -->
            <div class="live-transcription-view" :class="{ empty: !currentAnswer.trim() && !interimTranscript.trim() }">
              <span v-if="currentAnswer.trim() || interimTranscript.trim()" class="transcript-text">
                {{ currentAnswer }}<span v-if="interimTranscript.trim()" class="interim-text"> {{ interimTranscript }}</span>
              </span>
              <span v-else class="transcript-placeholder">
                {{ isRecording ? '请开口阐述您的回答，语音将在此实时呈现...' : '点击下方「🎙️ 开始口述作答」，对着麦克风阐述您的技术架构与实战经验...' }}
              </span>
            </div>

            <!-- 作答核心操控条 -->
            <div class="console-action-bar">
              <!-- 录音未开启状态 -->
              <template v-if="!isRecording">
                <el-button
                  type="primary"
                  size="large"
                  class="start-voice-btn"
                  :disabled="submittingAnswer || isCompleted"
                  @click="startSpeechRecording"
                >
                  <span class="btn-icon">🎙️</span>
                  <span>{{ currentAnswer.trim() ? '继续语音补充' : '开始口述作答 (开启麦克风)' }}</span>
                </el-button>
                <el-button
                  v-if="currentAnswer.trim()"
                  type="success"
                  size="large"
                  class="submit-voice-btn"
                  :loading="submittingAnswer"
                  :disabled="isCompleted"
                  @click="handleSubmitAnswer"
                >
                  <span>确认作答完毕，提交追问 ➔</span>
                </el-button>
              </template>

              <!-- 录音正在进行中状态 -->
              <template v-else>
                <el-button
                  type="danger"
                  size="large"
                  class="recording-pulsing-btn"
                  :loading="submittingAnswer"
                  @click="stopAndSubmitSpeechAnswer"
                >
                  <span class="btn-icon">⏹️</span>
                  <span>作答完毕，立即提交本轮作答 ➔</span>
                </el-button>
                <el-button
                  type="info"
                  size="large"
                  plain
                  @click="stopSpeechRecordingOnly"
                >
                  暂停收音
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="handleClearSpeechAnswer"
                >
                  清空重说
                </el-button>
              </template>
            </div>
          </div>

          <!-- 候选人手撕代码与沙箱评测工作台 -->
          <div v-else class="candidate-coding-console">
            <!-- 编码工具栏 -->
            <div class="coding-toolbar">
              <div class="tool-left">
                <el-radio-group v-model="liveCodeLang" size="small" @change="handleLiveLangChange">
                  <el-radio-button label="java">☕ Java (JDK 17)</el-radio-button>
                  <el-radio-button label="python">🐍 Python 3</el-radio-button>
                  <el-radio-button label="javascript">⚡ JS (Node)</el-radio-button>
                </el-radio-group>
                <el-dropdown trigger="click" @command="fetchSocraticHint" class="hint-dropdown">
                  <el-button size="small" type="warning" plain :loading="hintLoading">
                    💡 考官启发提示 ▼
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="1">Level 1: 算法核心思路与选型启发</el-dropdown-item>
                      <el-dropdown-item command="2">Level 2: 边界用例与异常防御提示</el-dropdown-item>
                      <el-dropdown-item command="3">Level 3: 代码结构微调与复杂度优化</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <div class="tool-right">
                <el-button size="small" @click="resetLiveCode">🔄 模板</el-button>
                <el-button type="primary" size="small" :loading="liveSandboxTesting" @click="testRunSandbox">
                  ⚡ 沙箱测试试跑
                </el-button>
                <el-button type="success" size="small" :loading="submittingAnswer" :disabled="isCompleted" @click="handleInterviewCodeSubmit">
                  🚀 提交本题代码 ➔
                </el-button>
              </div>
            </div>

            <!-- 苏格拉底式考官启发卡片 -->
            <div v-if="currentHintText" class="socratic-hint-banner">
              <div class="hint-banner-header">
                <span class="hint-tag">💡 考官 Level {{ hintLevel }} 启发提示（不剧透最终代码）：</span>
                <el-button link type="info" size="small" @click="currentHintText = ''">✕ 关闭</el-button>
              </div>
              <div class="hint-banner-body">{{ currentHintText }}</div>
            </div>

            <!-- 代码编辑框 -->
            <div class="coding-editor-wrapper">
              <div class="code-editor-header">
                <span class="file-name">Solution.{{ liveCodeLang === 'java' ? 'java' : (liveCodeLang === 'python' ? 'py' : 'js') }}</span>
                <span class="editor-sub">支持内存动态编译、时空复杂度推演与防注入沙箱</span>
              </div>
              <el-input
                v-model="liveCode"
                type="textarea"
                :rows="11"
                class="code-textarea"
                placeholder="在此编写算法与解题代码..."
                spellcheck="false"
              />
            </div>

            <!-- 沙箱测试实时输出面板 -->
            <div class="live-sandbox-panel" v-if="liveSandboxResult || liveSandboxTesting">
              <div class="sandbox-panel-header">
                <span class="panel-title">🖥️ 实时测试沙箱输出：</span>
                <div class="panel-meta" v-if="liveSandboxResult">
                  <el-tag :type="liveSandboxResult.status === 'SUCCESS' ? 'success' : (liveSandboxResult.status === 'SECURITY_VIOLATION' ? 'danger' : 'warning')" size="small">
                    {{ liveSandboxResult.status }}
                  </el-tag>
                  <span class="meta-time" v-if="liveSandboxResult.executionTimeMs">⏱️ {{ liveSandboxResult.executionTimeMs }}ms</span>
                  <span class="meta-exit">Exit: {{ liveSandboxResult.exitCode ?? 0 }}</span>
                  <el-button link type="info" size="small" @click="liveSandboxResult = null">✕</el-button>
                </div>
              </div>
              <div class="sandbox-panel-body">
                <div v-if="liveSandboxTesting" class="testing-tip">
                  <span class="pulse-point"></span> 沙箱隔离运行测试用例中...
                </div>
                <pre v-else class="output-pre"><code :class="{ 'error-out': liveSandboxResult.status !== 'SUCCESS' }">{{ liveSandboxResult.stdout || liveSandboxResult.stderr || '[沙箱执行完毕，无控制台打印]' }}</code></pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 往轮问答与大厂标杆复盘抽屉 (保持全量教育诊断价值) -->
    <el-drawer
      v-model="historyDrawerVisible"
      title="📋 模拟面试往轮实录与大厂专家深度复盘"
      direction="btt"
      size="65%"
      class="history-drawer-custom"
    >
      <div class="history-drawer-content">
        <div v-if="answeredTurns.length === 0" class="empty-history">
          <el-empty description="当前为第 1 轮开篇考核，完成答题后将在此沉淀大厂标杆满分范式与雷达评分！" />
        </div>

        <div v-for="turn in answeredTurns" :key="turn.id" class="drawer-turn-card">
          <div class="turn-card-header">
            <div class="turn-meta">
              <el-tag effect="dark" type="primary">第 {{ turn.turnNum }} 轮</el-tag>
              <el-tag effect="plain" type="info">{{ getDepthText(turn.depthLevel) }}</el-tag>
              <span class="turn-dim">考核维度：{{ turn.dimension || currentDimension }}</span>
            </div>
            <div class="turn-score-badge" v-if="turn.turnScore != null">
              <span>考官评定：</span>
              <b :class="getScoreColor(turn.turnScore)">{{ turn.turnScore }} 分</b>
            </div>
          </div>

          <div class="drawer-qa-section">
            <div class="qa-item q">
              <span class="qa-tag">考官提问</span>
              <div class="qa-text">{{ turn.question }}</div>
            </div>
            <div class="qa-item a">
              <span class="qa-tag">口述作答实录</span>
              <div class="qa-text">{{ turn.userAnswer }}</div>
            </div>
          </div>

          <!-- 深度复盘与标杆 -->
          <div v-if="turn.aiFeedback" class="drawer-feedback-box">
            <div class="fb-item">
              <div class="fb-title">💡 考官深度诊断剖析</div>
              <div class="fb-body">{{ turn.aiFeedback }}</div>
            </div>
            <div v-if="turn.standardReference" class="fb-item standard">
              <div class="fb-title">🏆 大厂标杆满分范式</div>
              <div class="fb-body">{{ turn.standardReference }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 文本微调弹窗（针对语音转录时的极客生僻英文/专有名词容错） -->
    <el-dialog
      v-model="editDialogVisible"
      title="✏️ 口述作答文本微调（校准专有名词）"
      width="540px"
      append-to-body
    >
      <div class="edit-dialog-tip">
        提示：系统采用语音实时转录，若 Netty、B+Tree、CAS 等专业英文缩写有误差，可在此微调校准后提交。
      </div>
      <el-input
        v-model="currentAnswer"
        type="textarea"
        :rows="5"
        resize="none"
        placeholder="微调您的作答内容..."
      />
      <template #footer>
        <el-button @click="editDialogVisible = false">完成校准</el-button>
        <el-button type="primary" @click="editDialogVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInterviewDetail,
  submitInterviewAnswer,
  submitInterviewCode,
  runInterviewSandbox,
  getInterviewHint,
  finishInterview
} from '@/api/interview.js'
import { createVirtualCameraStream } from '@/utils/virtualCamera.js'

const route = useRoute()
const router = useRouter()
const sessionId = route.params.id

const loading = ref(true)
const submittingAnswer = ref(false)
const finishing = ref(false)
const historyDrawerVisible = ref(false)
const editDialogVisible = ref(false)

const sessionData = ref({
  turns: []
})

const currentAnswer = ref('')

// ==================== 手撕代码与沙箱评测逻辑 ====================
const answerMode = ref('voice') // 'voice' | 'code'

// 监听作答模式切换，切换到手撕代码时若麦克风正在录音则自动安全暂停收音
watch(answerMode, (newMode) => {
  if (newMode === 'code' && isRecording.value) {
    stopSpeechRecordingOnly()
  }
})
const liveCodeLang = ref('java')
const liveSandboxTesting = ref(false)
const liveSandboxResult = ref(null)
const hintLoading = ref(false)
const currentHintText = ref('')
const hintLevel = ref(1)

const interviewCodeTemplates = {
  java: `public class Solution {\n    public static void main(String[] args) {\n        // 编写解题逻辑与测试用例\n        System.out.println("Solution initialized");\n    }\n}`,
  python: `def solve():\n    # 编写解题逻辑与测试用例\n    print("Solution initialized")\n\nif __name__ == '__main__':\n    solve()`,
  javascript: `function solve() {\n    // 编写解题逻辑与测试用例\n    console.log("Solution initialized");\n}\nsolve();`
}

const liveCode = ref(interviewCodeTemplates.java)

const handleLiveLangChange = (lang) => {
  liveCode.value = interviewCodeTemplates[lang] || ''
  liveSandboxResult.value = null
}

const resetLiveCode = () => {
  liveCode.value = interviewCodeTemplates[liveCodeLang.value] || ''
  liveSandboxResult.value = null
}

const fetchSocraticHint = async (cmd) => {
  const lvl = Number(cmd) || 1
  try {
    hintLoading.value = true
    hintLevel.value = lvl
    const res = await getInterviewHint({
      sessionId: String(sessionId),
      turnId: currentTurn.value?.id,
      hintLevel: lvl,
      currentCode: liveCode.value
    })
    if (res && res.code === 200 && res.data) {
      currentHintText.value = res.data.hint
      ElMessage.success(`已获取考官 Level ${lvl} 启发式提示！`)
    } else {
      ElMessage.warning(res?.msg || '获取提示失败')
    }
  } catch (err) {
    ElMessage.error('调用启发式接口失败: ' + (err.message || '网络异常'))
  } finally {
    hintLoading.value = false
  }
}

const testRunSandbox = async () => {
  if (!liveCode.value.trim()) {
    ElMessage.warning('请输入待测试的代码！')
    return
  }
  try {
    liveSandboxTesting.value = true
    liveSandboxResult.value = null
    const res = await runInterviewSandbox({
      language: liveCodeLang.value,
      sourceCode: liveCode.value
    })
    if (res && res.code === 200 && res.data) {
      liveSandboxResult.value = res.data
      if (res.data.status === 'SUCCESS') {
        ElMessage.success('沙箱测试通过！')
      } else {
        ElMessage.warning('沙箱测试未通过: ' + res.data.status)
      }
    } else {
      ElMessage.error(res?.msg || '沙箱执行出错')
    }
  } catch (err) {
    ElMessage.error('沙箱执行异常: ' + (err.message || '网络错误'))
  } finally {
    liveSandboxTesting.value = false
  }
}

const handleInterviewCodeSubmit = async () => {
  if (!liveCode.value.trim()) {
    ElMessage.warning('代码内容不能为空！')
    return
  }
  try {
    await ElMessageBox.confirm(
      '确定提交当前手撕代码？提交后 AI 考官将执行深度时空复杂度推演与架构异味审计，并推进至下一轮考题。',
      '提交手撕代码确认',
      { confirmButtonText: '确定提交', cancelButtonText: '继续调试', type: 'info' }
    )
    submittingAnswer.value = true
    const res = await submitInterviewCode({
      sessionId: String(sessionId),
      turnId: currentTurn.value?.id,
      problemTitle: currentTurn.value?.question || '算法设计与手撕代码',
      language: liveCodeLang.value,
      userCode: liveCode.value
    })
    if (res && res.code === 200 && res.data) {
      ElMessage.success('手撕代码提交成功，已完成沙箱评测与考官审计！')
      liveCode.value = interviewCodeTemplates[liveCodeLang.value]
      liveSandboxResult.value = null
      currentHintText.value = ''
      answerMode.value = 'voice'
      await loadSession()
    } else {
      ElMessage.error(res?.msg || '提交代码失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('提交代码异常: ' + (e.message || '网络错误'))
    }
  } finally {
    submittingAnswer.value = false
  }
}

// 计时器
const timerSeconds = ref(0)
let timerInterval = null
let virtualAudioInterval = null

// 音视频与硬件设备状态
const candidateVideoRef = ref(null)
const cameraActive = ref(true)
const micActive = ref(true)
let candidateStream = null
let candidateAudioCtx = null
let candidateAnalyser = null
let animFrameId = null
const audioEnergy = ref(0)

// AI 面试官数字人与发音 (TTS) 状态
const isInterviewerSpeaking = ref(false)
let currentUtterance = null
let ttsHeartbeatInterval = null

// 候选人语音识别 (STT) 状态
const isRecording = ref(false)
let speechRecognitionInstance = null
const interimTranscript = ref('')

const currentTurn = computed(() => {
  const turns = sessionData.value.turns || []
  if (turns.length === 0) return null
  // 优先取尚未作答的最新轮次，否则取最后一轮
  const unanswered = turns.find(t => !t.userAnswer)
  return unanswered || turns[turns.length - 1]
})

const currentTurnNum = computed(() => currentTurn.value?.turnNum || 1)
const currentDepth = computed(() => currentTurn.value?.depthLevel || 1)
const currentDimension = computed(() => currentTurn.value?.dimension || '核心技术架构')

const currentStage = computed(() => {
  if (currentTurn.value?.stage) return currentTurn.value.stage
  const num = currentTurnNum.value
  const total = sessionData.value?.totalTurns || 20
  if (num === 1) return 1
  if (num <= Math.min(11, Math.floor(total * 0.55))) return 2
  return 3
})

const remainingSeconds = computed(() => Math.max(0, 3600 - timerSeconds.value))
const isCompleted = computed(() => sessionData.value.status === 2 || sessionData.value.status === 3)
const isTerminated = computed(() => sessionData.value.status === 3)

const answeredTurns = computed(() => {
  const turns = sessionData.value.turns || []
  return turns.filter(t => Boolean(t.userAnswer && t.userAnswer.trim()))
})

const answeredTurnsCount = computed(() => answeredTurns.value.length)

// AI 面试官活动状态机
const aiActivityState = computed(() => {
  if (submittingAnswer.value) return 'thinking'
  if (isInterviewerSpeaking.value) return 'speaking'
  if (isRecording.value) return 'listening'
  return 'listening'
})

const getPersonaMeta = computed(() => {
  const style = sessionData.value.interviewerStyle || 'p7_architect'
  if (style === 'bytedance_tech') {
    return { icon: '💻', name: '字节跳动技术专家' }
  }
  if (style === 'gentle_hr') {
    return { icon: '🤝', name: '资深大厂 HRBP' }
  }
  if (style === 'standard') {
    return { icon: '⚖️', name: '大厂评审委员会' }
  }
  return { icon: '⚡', name: '阿里 P7 资深架构师' }
})

const getDepthText = (depth) => {
  if (depth === 2) return 'Level 2 底层原理深挖'
  if (depth === 3) return 'Level 3 线上极限排障'
  return 'Level 1 概念摸底'
}

const getScoreColor = (score) => {
  if (score >= 85) return 'score-high'
  if (score >= 70) return 'score-mid'
  return 'score-low'
}

const formatTimer = (secs) => {
  const m = Math.floor(secs / 60)
  const s = secs % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(11, 19)
}

const goToReport = () => {
  const targetId = sessionId || (sessionData.value && sessionData.value.id)
  if (!targetId) {
    ElMessage.warning('面试场次信息缺失，无法跳转报告')
    return
  }
  router.push(`/interview/report/${targetId}`)
}

const handleBack = () => {
  router.push({ name: 'interviewIndex' })
}

// 动态获取 VU-Meter 能量条高度
const getDynamicBarHeight = (index) => {
  if (!micActive.value || (!isRecording.value && audioEnergy.value < 5)) {
    return 4
  }
  const factor = (Math.sin(Date.now() / 150 + index) + 1) / 2
  const base = Math.max(4, (audioEnergy.value / 100) * 22 * factor)
  return Math.round(base)
}

// 初始化候选人音视频流与能量监听
const initCandidateMedia = async () => {
  const isSimulation = sessionStorage.getItem('interview_camera_simulation') === '1'
  const hasHardwareApi = Boolean(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)

  // 辅助函数：启动虚拟全息摄像头流
  const startVirtualStream = () => {
    try {
      const stream = createVirtualCameraStream('候选人（我）')
      candidateStream = stream
      cameraActive.value = true
      micActive.value = true
      if (candidateVideoRef.value) {
        candidateVideoRef.value.srcObject = stream
      }
      // 模拟声浪律动
      let tick = 0
      if (virtualAudioInterval) {
        clearInterval(virtualAudioInterval)
      }
      virtualAudioInterval = setInterval(() => {
        tick++
        audioEnergy.value = Math.round(20 + Math.sin(tick * 0.2) * 15 + Math.random() * 10)
      }, 100)
    } catch (e) {
      console.warn('启动虚拟摄像头流异常:', e)
      cameraActive.value = false
    }
  }

  if (isSimulation || !hasHardwareApi) {
    startVirtualStream()
    return
  }

  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: true
    })
    candidateStream = stream
    cameraActive.value = true

    if (candidateVideoRef.value) {
      candidateVideoRef.value.srcObject = stream
    }

    // 绑定 Web Audio API 分析麦克风音量能量 (VU-Meter)
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext
      candidateAudioCtx = new AudioCtx()
      const source = candidateAudioCtx.createMediaStreamSource(stream)
      candidateAnalyser = candidateAudioCtx.createAnalyser()
      candidateAnalyser.fftSize = 128
      source.connect(candidateAnalyser)

      const bufferLength = candidateAnalyser.frequencyBinCount
      const dataArray = new Uint8Array(bufferLength)

      const trackVolume = () => {
        if (!candidateAnalyser) return
        candidateAnalyser.getByteFrequencyData(dataArray)
        let sum = 0
        for (let i = 0; i < bufferLength; i++) {
          sum += dataArray[i]
        }
        const avg = sum / bufferLength
        audioEnergy.value = Math.min(100, Math.round((avg / 128) * 100))
        animFrameId = requestAnimationFrame(trackVolume)
      }
      trackVolume()
    } catch (e) {
      console.warn('候选人音频分析器初始化忽略:', e)
    }
  } catch (err) {
    console.warn('获取候选人真实摄像头失败，自动降级为虚拟全息摄像头:', err)
    startVirtualStream()
  }
}

const toggleCamera = () => {
  if (!candidateStream) {
    ElMessage.info('当前为无摄像头模拟连线模式')
    return
  }
  const videoTracks = candidateStream.getVideoTracks()
  if (videoTracks.length > 0) {
    videoTracks[0].enabled = !videoTracks[0].enabled
    cameraActive.value = videoTracks[0].enabled
    ElMessage.info(cameraActive.value ? '摄像头已开启' : '摄像头已关闭')
  } else {
    ElMessage.warning('当前设备未检测到可用的视频轨道')
  }
}

const toggleMic = () => {
  if (!candidateStream) {
    micActive.value = !micActive.value
    return
  }
  const audioTracks = candidateStream.getAudioTracks()
  if (audioTracks.length > 0) {
    audioTracks[0].enabled = !audioTracks[0].enabled
    micActive.value = audioTracks[0].enabled
    ElMessage.info(micActive.value ? '麦克风已开启' : '麦克风已静音')
  }
}

// ==================== 微软晓晓 (Xiaoxiao Neural TTS) 智能语音引擎 ====================
const ttsVoiceLabel = ref('微软晓晓 (载入中...)')
const isXiaoxiaoReady = ref(false)
let currentXiaoxiaoVoice = null

// 智能检测并优先绑定微软晓晓音色
const initXiaoxiaoVoice = () => {
  if (!('speechSynthesis' in window)) {
    ttsVoiceLabel.value = '未支持语音合成'
    return
  }

  const findBestVoice = () => {
    const voices = window.speechSynthesis.getVoices()
    if (!voices || voices.length === 0) return false

    // 1. 最高优先级：精准匹配微软晓晓（包括 Edge 在线自然语音与 Windows 本地自然语音包）
    // 识别常见标识：'Microsoft Xiaoxiao Online (Natural) - Chinese (Mainland)', 'Microsoft Xiaoxiao - Chinese (Simplified, PRC)'
    const xiaoxiao = voices.find(v =>
      (v.name.includes('Xiaoxiao') || v.name.includes('晓晓') || (v.voiceURI && v.voiceURI.includes('Xiaoxiao'))) &&
      (v.lang.includes('zh') || v.lang.includes('CN'))
    ) || voices.find(v =>
      v.name.includes('Xiaoxiao') || v.name.includes('晓晓')
    )

    if (xiaoxiao) {
      currentXiaoxiaoVoice = xiaoxiao
      isXiaoxiaoReady.value = true
      ttsVoiceLabel.value = '微软晓晓 (Neural 自然女声)'
      return true
    }

    // 2. 次高优先级：微软其他自然神经语音（如晓伊 Xiaoyi / 云希 Yunxi / Natural / Online）
    const naturalZh = voices.find(v =>
      (v.name.includes('Natural') || v.name.includes('Online')) &&
      (v.lang.includes('zh') || v.lang.includes('CN'))
    ) || voices.find(v =>
      (v.name.includes('Yunxi') || v.name.includes('云希') || v.name.includes('Xiaoyi') || v.name.includes('晓伊')) &&
      (v.lang.includes('zh') || v.lang.includes('CN'))
    )

    if (naturalZh) {
      currentXiaoxiaoVoice = naturalZh
      isXiaoxiaoReady.value = true
      ttsVoiceLabel.value = `${naturalZh.name} (自然语音)`
      return true
    }

    // 3. 兼容兜底：高品质中文语音（Google 普通话、系统原生 zh-CN 语音）
    const fallbackZh = voices.find(v =>
      v.name.includes('Google') && (v.lang.includes('zh') || v.lang.includes('CN'))
    ) || voices.find(v =>
      v.lang === 'zh-CN' || v.lang === 'zh_CN' || (v.lang && v.lang.startsWith('zh'))
    )

    if (fallbackZh) {
      currentXiaoxiaoVoice = fallbackZh
      isXiaoxiaoReady.value = false
      ttsVoiceLabel.value = `系统中文 (${fallbackZh.name})`
      return true
    }

    return false
  }

  // 尝试立即获取
  if (!findBestVoice()) {
    // Chromium 异步加载声音钩子
    window.speechSynthesis.onvoiceschanged = () => {
      findBestVoice()
    }
  }
}

// Web Speech API - AI 面试官语音朗读 (TTS 微软晓晓)
const speakQuestionText = (text) => {
  if (!('speechSynthesis' in window)) {
    console.warn('当前浏览器不支持 Web Speech 语音合成')
    return
  }
  if (ttsHeartbeatInterval) {
    clearInterval(ttsHeartbeatInterval)
    ttsHeartbeatInterval = null
  }
  window.speechSynthesis.cancel()
  if (!text) return

  // 确保音色已就绪
  if (!currentXiaoxiaoVoice) {
    initXiaoxiaoVoice()
  }

  const utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'

  if (currentXiaoxiaoVoice) {
    utterance.voice = currentXiaoxiaoVoice
  }

  // 晓晓音色面试官专用工程级声学调优：
  // rate: 1.02（利落从容，不拖沓）
  // pitch: 1.05（亲切干练的大厂女考官基频）
  utterance.rate = 1.02
  utterance.pitch = 1.05

  utterance.onstart = () => {
    isInterviewerSpeaking.value = true
    // BUG-32: Chromium 15秒超长语音朗读静默暂停修复（周期性心跳触发 resume 维持合成管道活性）
    if (ttsHeartbeatInterval) clearInterval(ttsHeartbeatInterval)
    ttsHeartbeatInterval = setInterval(() => {
      if (window.speechSynthesis && window.speechSynthesis.speaking) {
        window.speechSynthesis.pause()
        window.speechSynthesis.resume()
      } else {
        if (ttsHeartbeatInterval) {
          clearInterval(ttsHeartbeatInterval)
          ttsHeartbeatInterval = null
        }
      }
    }, 10000)
  }
  utterance.onend = () => {
    isInterviewerSpeaking.value = false
    if (ttsHeartbeatInterval) {
      clearInterval(ttsHeartbeatInterval)
      ttsHeartbeatInterval = null
    }
  }
  utterance.onerror = (e) => {
    console.warn('TTS 语音合成提示:', e)
    isInterviewerSpeaking.value = false
    if (ttsHeartbeatInterval) {
      clearInterval(ttsHeartbeatInterval)
      ttsHeartbeatInterval = null
    }
  }

  currentUtterance = utterance
  window.speechSynthesis.speak(utterance)
}

const toggleTtsCurrentQuestion = () => {
  if (isInterviewerSpeaking.value) {
    window.speechSynthesis.cancel()
    isInterviewerSpeaking.value = false
  } else if (currentTurn.value?.question) {
    speakQuestionText(currentTurn.value.question)
  }
}

// 试听晓晓问候音
const previewXiaoxiaoVoice = () => {
  speakQuestionText('同学你好，我是今天的AI主考官晓晓。很高兴与你连线，请放轻松，祝你在接下来的模拟面试中发挥出最佳水平！')
}

// Web Speech API - 候选人实时语音识别 (STT)
const startSpeechRecording = () => {
  // 如果考官还在念题目，先暂停考官发音
  if (isInterviewerSpeaking.value) {
    window.speechSynthesis.cancel()
    isInterviewerSpeaking.value = false
  }

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    ElMessage.warning('您的浏览器暂未开放 Web 语音识别接口（推荐使用 Chrome / Edge 浏览器）')
    // 允许通过微调弹窗打字补充
    editDialogVisible.value = true
    return
  }

  try {
    const recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'
    recognition.continuous = true
    recognition.interimResults = true

    recognition.onstart = () => {
      isRecording.value = true
      ElMessage.success('麦克风已开启，请开口阐述您的回答...')
    }

    recognition.onresult = (event) => {
      let interim = ''
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) {
          const finalStr = event.results[i][0].transcript
          currentAnswer.value = (currentAnswer.value ? currentAnswer.value + ' ' : '') + finalStr.trim()
        } else {
          interim += event.results[i][0].transcript
        }
      }
      interimTranscript.value = interim
    }

    recognition.onerror = (event) => {
      console.warn('语音识别事件异常:', event.error)
      if (event.error !== 'no-speech') {
        ElMessage.warning('麦克风收音提示：' + event.error)
      }
    }

    recognition.onend = () => {
      // BUG-29: 若非主动终止，延时平滑重连避免 Chromium 立即 start 报 InvalidStateError
      if (isRecording.value) {
        setTimeout(() => {
          if (isRecording.value) {
            try {
              recognition.start()
            } catch (e) {
              // 已经在启动状态则忽略
            }
          }
        }, 250)
      }
    }

    speechRecognitionInstance = recognition
    recognition.start()
  } catch (err) {
    isRecording.value = false
    ElMessage.error('启动麦克风失败：' + (err.message || '权限被拒绝'))
  }
}

const stopSpeechRecordingOnly = () => {
  isRecording.value = false
  interimTranscript.value = ''
  if (speechRecognitionInstance) {
    try {
      speechRecognitionInstance.stop()
    } catch (e) {}
    speechRecognitionInstance = null
  }
  ElMessage.info('已暂停麦克风收音')
}

const handleClearSpeechAnswer = () => {
  currentAnswer.value = ''
  ElMessage.info('已清空作答草稿，可重新口述')
}

// 提交回答并迎接连环追问
const handleSubmitAnswer = async () => {
  if (!currentTurn.value) return
  const text = currentAnswer.value.trim()
  if (!text) {
    ElMessage.warning('尚未检测到有效的口述作答内容，请开启麦克风作答！')
    return
  }

  // 停止收音
  stopSpeechRecordingOnly()

  try {
    submittingAnswer.value = true
    // BUG-30: 严禁对 64-bit Snowflake ID 使用 Number() 强转导致低位精度截断
    const res = await submitInterviewAnswer({
      sessionId: String(sessionId),
      turnId: currentTurn.value.id,
      userAnswer: text
    })
    if (res && res.code === 200 && res.data) {
      ElMessage.success('本轮口述作答已提交，AI 面试官已完成评分与追问组织！')
      currentAnswer.value = ''
      interimTranscript.value = ''
      await loadSession()

      // 若考核已达成终局
      if (sessionData.value.status === 2) {
        ElMessageBox.alert(
          '恭喜您已圆满完成全流程视频面试考核！大厂评审委员会已完成终局职级裁决与六维能力雷达图评定。',
          '模拟面试已完成',
          {
            confirmButtonText: '查看终局报告',
            type: 'success',
            callback: () => goToReport()
          }
        )
      } else {
        // 进入下一轮题目，自动触发 AI 面试官语音出题
        nextTick(() => {
          if (currentTurn.value?.question && !currentTurn.value.userAnswer) {
            setTimeout(() => {
              speakQuestionText(currentTurn.value.question)
            }, 600)
          }
        })
      }
    } else {
      ElMessage.error(res?.msg || '提交作答失败')
    }
  } catch (err) {
    ElMessage.error('提交作答失败：' + (err.message || '系统繁忙'))
  } finally {
    submittingAnswer.value = false
  }
}

const stopAndSubmitSpeechAnswer = () => {
  handleSubmitAnswer()
}

const loadSession = async () => {
  try {
    loading.value = true
    const res = await getInterviewDetail(sessionId)
    if (res && res.code === 200 && res.data) {
      sessionData.value = res.data
      // BUG-33: 仅在初次载入或已交卷时对齐耗时，严禁在答题过程中用 0 或未落库时间重置学员计时器
      if (timerSeconds.value === 0 && res.data.durationSeconds) {
        timerSeconds.value = res.data.durationSeconds
      } else if (res.data.status === 2 || res.data.status === 3) {
        timerSeconds.value = res.data.durationSeconds || timerSeconds.value
      }

      // 如果当前题目未作答且未交卷，首次自动朗读提问
      if (res.data.status === 1 && currentTurn.value?.question && !currentTurn.value.userAnswer) {
        nextTick(() => {
          setTimeout(() => {
            speakQuestionText(currentTurn.value.question)
          }, 800)
        })
      } else if (res.data.status === 2) {
        ElMessage.info('本场面试已交卷完成，可随时查看能力诊断报告')
      }
    } else if (res && res.code !== 200) {
      ElMessage.error(res?.msg || '加载考场详情失败')
      router.push('/interview')
    }
  } catch (err) {
    ElMessage.error('加载考场详情异常：' + (err.message || '网络错误'))
    router.push('/interview')
  } finally {
    loading.value = false
  }
}

const handleFinishInterview = () => {
  const turns = sessionData.value.turns || []
  const hasAnyAnswer = turns.some(t => Boolean(t.userAnswer && t.userAnswer.trim()))
  const confirmMsg = hasAnyAnswer
    ? '确认现在交卷并结束面试？系统将立即触发阿里/字节多维评审委员会终局裁决与六维能力雷达图生成。'
    : '【注意】：您当前尚未作答任何题目，直接交卷将按【缺考/未达标】终局裁定且综合得分为 0。确认现在交卷吗？'

  ElMessageBox.confirm(
    confirmMsg,
    hasAnyAnswer ? '交卷终审确认' : '缺考直接交卷警示',
    {
      confirmButtonText: '立即交卷',
      cancelButtonText: '继续答题',
      type: hasAnyAnswer ? 'warning' : 'danger'
    }
  ).then(async () => {
    try {
      finishing.value = true
      // 停止音视频与识别
      stopSpeechRecordingOnly()
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }
      const res = await finishInterview(sessionId)
      if (res && res.code === 200 && res.data) {
        ElMessage.success('终局报告已生成！')
        goToReport()
      } else {
        ElMessage.error(res?.msg || '交卷失败')
      }
    } catch (err) {
      ElMessage.error('交卷异常：' + (err.message || '网络错误'))
    } finally {
      finishing.value = false
    }
  }).catch(() => {})
}

onMounted(() => {
  loadSession()
  initCandidateMedia()
  initXiaoxiaoVoice()
  timerInterval = setInterval(() => {
    if (!isCompleted.value) {
      timerSeconds.value++
    }
  }, 1000)
})

onBeforeUnmount(() => {
  if (timerInterval) clearInterval(timerInterval)
  // BUG-27: 彻底清理虚拟音频模拟定时器
  if (virtualAudioInterval) clearInterval(virtualAudioInterval)
  // BUG-32: 清理 TTS 心跳计时器
  if (ttsHeartbeatInterval) clearInterval(ttsHeartbeatInterval)
  if (animFrameId) cancelAnimationFrame(animFrameId)
  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel()
  }
  if (speechRecognitionInstance) {
    try {
      speechRecognitionInstance.stop()
    } catch (e) {}
    speechRecognitionInstance = null
  }
  if (candidateAudioCtx && candidateAudioCtx.state !== 'closed') {
    try {
      candidateAudioCtx.close()
    } catch (e) {}
  }
  if (candidateStream) {
    try {
      // BUG-28: 彻底销毁虚拟摄像头内部 canvas requestAnimationFrame 渲染循环
      if (typeof candidateStream._stopVirtualAnimation === 'function') {
        candidateStream._stopVirtualAnimation()
      }
      candidateStream.getTracks().forEach(t => t.stop())
    } catch (e) {}
  }
})
</script>

<style scoped>
.interview-room-container {
  height: calc(100vh - 72px);
  display: flex;
  flex-direction: column;
  background: #090d16;
  color: #f8fafc;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* 考情 HUD 状态栏 */
.interview-hud {
  height: 52px;
  background: #0d1322;
  color: #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
  z-index: 10;
}

.hud-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-btn {
  color: #94a3b8;
  font-size: 13px;
}

.back-btn:hover {
  color: #38bdf8;
}

.hud-divider {
  width: 1px;
  height: 18px;
  background: #334155;
}

.persona-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
  color: #38bdf8;
  background: rgba(56, 189, 248, 0.1);
  padding: 4px 10px;
  border-radius: 12px;
  border: 1px solid rgba(56, 189, 248, 0.2);
}

.dimension-tag {
  background: rgba(30, 41, 59, 0.8);
  border-color: #334155;
}

.hud-center {
  display: flex;
  align-items: center;
}

.hud-center {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stage-hud {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  background: rgba(15, 23, 42, 0.45);
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.stage-step {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #64748b;
  transition: all 0.3s;
}

.stage-step .step-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: rgba(100, 116, 139, 0.3);
  font-size: 10px;
  font-weight: 700;
  color: #94a3b8;
}

.stage-step.active {
  color: #93c5fd;
}

.stage-step.current {
  color: #38bdf8;
  font-weight: 700;
}

.stage-step.current .step-badge {
  background: #0284c7;
  color: #ffffff;
  box-shadow: 0 0 8px rgba(56, 189, 248, 0.6);
}

.stage-arrow {
  color: #475569;
  font-size: 11px;
}

.depth-hud {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.depth-label {
  color: #64748b;
}

.depth-step {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #64748b;
  transition: all 0.3s;
}

.depth-step.active {
  color: #38bdf8;
  font-weight: 600;
}

.depth-step .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #475569;
}

.depth-step.active .dot {
  background: #38bdf8;
  box-shadow: 0 0 6px #38bdf8;
}

.arrow {
  color: #475569;
}

.hud-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hud-timer {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: monospace;
  font-size: 14px;
  color: #fbbf24;
  font-weight: 600;
  background: rgba(251, 191, 36, 0.1);
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid rgba(251, 191, 36, 0.2);
}

.hud-timer.warning-timer {
  color: #f43f5e;
  border-color: rgba(244, 63, 94, 0.4);
  background: rgba(244, 63, 94, 0.15);
  animation: timerPulse 1s infinite alternate;
}

@keyframes timerPulse {
  from { opacity: 0.8; }
  to { opacity: 1; transform: scale(1.03); }
}

.turn-progress {
  font-size: 13px;
  color: #94a3b8;
}

.turn-progress b {
  color: #f8fafc;
}

/* 核心双人视频会议舞台 */
.video-conference-stage {
  flex: 1;
  padding: 14px 18px 18px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.conference-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 16px;
  height: 100%;
}

/* 舞台卡片基底 */
.stage-card {
  background: #0f172a;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.07);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
}

.stage-header {
  height: 44px;
  padding: 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(15, 23, 42, 0.95);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
  z-index: 5;
}

.stream-status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.live-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.live-indicator.candidate.recording {
  background: #ef4444;
  box-shadow: 0 0 10px #ef4444;
  animation: pulse-red 1s infinite;
}

@keyframes pulse-red {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.2); }
}

.stream-title {
  font-size: 13px;
  font-weight: 600;
  color: #f1f5f9;
}

.ai-state-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.ai-state-badge.speaking {
  background: rgba(14, 165, 233, 0.2);
  color: #38bdf8;
  border: 1px solid rgba(14, 165, 233, 0.4);
}

.ai-state-badge.listening {
  background: rgba(16, 185, 129, 0.2);
  color: #34d399;
  border: 1px solid rgba(16, 185, 129, 0.4);
}

.ai-state-badge.thinking {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.4);
  animation: pulse 1.2s infinite;
}

/* AI 面试官展示视窗 */
.interviewer-stage {
  border-color: rgba(56, 189, 248, 0.2);
}

.avatar-display-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: radial-gradient(circle at center, #1e293b 0%, #090d16 80%);
  overflow: hidden;
}

/* 声波能量光环 */
.soundwave-halo {
  position: absolute;
  width: 220px;
  height: 220px;
  pointer-events: none;
  opacity: 0.15;
  transition: opacity 0.3s;
}

.soundwave-halo.active {
  opacity: 0.85;
}

.halo-ring {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid #38bdf8;
}

.soundwave-halo.active .ring-1 {
  animation: halo-expand 2s infinite ease-out;
}

.soundwave-halo.active .ring-2 {
  animation: halo-expand 2s infinite 0.6s ease-out;
}

.soundwave-halo.active .ring-3 {
  animation: halo-expand 2s infinite 1.2s ease-out;
}

@keyframes halo-expand {
  0% { transform: scale(0.6); opacity: 1; }
  100% { transform: scale(1.4); opacity: 0; }
}

/* 动态矢量数字人形象 */
.digital-human-avatar {
  width: 220px;
  height: 240px;
  position: relative;
  z-index: 3;
}

.avatar-svg {
  width: 100%;
  height: 100%;
}

.avatar-head-group {
  animation: avatar-breathe 4s infinite ease-in-out;
  transform-origin: center bottom;
}

@keyframes avatar-breathe {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

.avatar-eyes {
  animation: eye-blink 5s infinite;
  transform-origin: 120px 115px;
}

@keyframes eye-blink {
  0%, 94%, 98%, 100% { transform: scaleY(1); }
  96% { transform: scaleY(0.1); }
}

.speaking-mouth .dynamic-open-mouth {
  animation: mouth-talk 0.25s infinite alternate ease-in-out;
  transform-origin: 120px 160px;
}

@keyframes mouth-talk {
  0% { transform: scaleY(0.4) scaleX(0.9); }
  100% { transform: scaleY(1.3) scaleX(1.1); }
}

.cyber-line {
  animation: line-glow 3s infinite linear;
}

@keyframes line-glow {
  0% { stroke-dashoffset: 0; }
  100% { stroke-dashoffset: 16; }
}

.avatar-floating-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  padding: 4px 10px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #e2e8f0;
  z-index: 4;
}

/* 考官发问字幕条 */
.interviewer-question-banner {
  background: rgba(15, 23, 42, 0.9);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 5;
}

.banner-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sub-dim {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 8px;
}

.tts-ctrl {
  display: flex;
  align-items: center;
  gap: 8px;
}

.voice-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
  color: #34d399;
  font-weight: 500;
}

.voice-badge.el-tag--info {
  background: rgba(148, 163, 184, 0.1);
  border-color: rgba(148, 163, 184, 0.25);
  color: #94a3b8;
}

.pulse-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #34d399;
  box-shadow: 0 0 6px #34d399;
  animation: pulse-green 1s infinite;
}

@keyframes pulse-green {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(1.3); }
}

.preview-voice-btn {
  font-size: 12px;
  color: #38bdf8;
  padding: 0 4px;
}

.preview-voice-btn:hover {
  color: #7dd3fc;
}

.speak-ctrl-btn {
  font-size: 12px;
}

.speak-ctrl-btn.active {
  color: #38bdf8;
  border-color: #38bdf8;
}

.question-voice-text {
  font-size: 15px;
  line-height: 1.6;
  color: #f8fafc;
  font-weight: 500;
  background: rgba(30, 41, 59, 0.5);
  padding: 10px 14px;
  border-radius: 8px;
  border-left: 3px solid #38bdf8;
}

/* 候选人视窗 */
.candidate-stage {
  border-color: rgba(255, 255, 255, 0.08);
}

.device-status-tags {
  display: flex;
  gap: 6px;
}

.candidate-video-viewport {
  height: 230px;
  background: #020617;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.candidate-video-elem {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
  transition: opacity 0.3s;
}

.candidate-video-elem.stream-hidden {
  opacity: 0;
}

.candidate-simulated-avatar {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #64748b;
}

.candidate-silhouette {
  font-size: 54px;
}

.sim-tip {
  font-size: 12px;
  color: #94a3b8;
}

.candidate-video-hud {
  position: absolute;
  bottom: 10px;
  left: 12px;
  right: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 5;
}

.candidate-audio-wave {
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(8px);
  padding: 4px 10px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.wave-tag {
  font-size: 11px;
  color: #94a3b8;
}

.wave-bars {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 22px;
}

.wave-bar {
  width: 3px;
  background: #38bdf8;
  border-radius: 2px;
  transition: height 0.06s ease-out;
}

.candidate-device-toggles {
  display: flex;
  gap: 8px;
}

/* 候选人实时语音口述转写工作台 */
.candidate-voice-console {
  flex: 1;
  background: #0d1322;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  gap: 10px;
}

.console-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-with-wave {
  display: flex;
  align-items: center;
  gap: 6px;
}

.record-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #64748b;
}

.record-dot.pulsing {
  background: #ef4444;
  box-shadow: 0 0 8px #ef4444;
  animation: pulse 1s infinite;
}

.con-title {
  font-size: 13px;
  color: #cbd5e1;
  font-weight: 500;
}

.char-badge {
  font-size: 12px;
  color: #64748b;
  margin-left: 8px;
}

/* 实时语音转录大字幕框 */
.live-transcription-view {
  flex: 1;
  min-height: 80px;
  max-height: 120px;
  overflow-y: auto;
  background: #070a12;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  color: #f1f5f9;
}

.live-transcription-view.empty {
  color: #64748b;
  font-style: italic;
  display: flex;
  align-items: center;
  justify-content: center;
}

.transcript-text {
  color: #38bdf8;
  font-weight: 500;
}

.interim-text {
  color: #94a3b8;
  font-style: italic;
}

.console-action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.start-voice-btn {
  flex: 1;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
  background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%);
  border: none;
}

.start-voice-btn:hover {
  background: linear-gradient(135deg, #0369a1 0%, #075985 100%);
}

.submit-voice-btn {
  height: 44px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 8px;
}

.recording-pulsing-btn {
  flex: 1;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
  background: #dc2626;
  border: none;
  animation: glow-red 1.2s infinite alternate;
}

@keyframes glow-red {
  0% { box-shadow: 0 0 4px rgba(220, 38, 38, 0.4); }
  100% { box-shadow: 0 0 16px rgba(220, 38, 38, 0.9); }
}

.btn-icon {
  margin-right: 6px;
  font-size: 16px;
}

/* 往轮复盘抽屉 */
.history-drawer-content {
  padding: 10px 20px 30px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drawer-turn-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
}

.turn-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.turn-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.turn-dim {
  font-size: 12px;
  color: #64748b;
}

.turn-score-badge {
  font-size: 14px;
  color: #334155;
}

.score-high { color: #16a34a; }
.score-mid { color: #ea580c; }
.score-low { color: #dc2626; }

.drawer-qa-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.qa-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.qa-tag {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
}

.qa-item.q .qa-text {
  background: #eff6ff;
  border: 1px solid #dbeafe;
  color: #1e3a8a;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.5;
}

.qa-item.a .qa-text {
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
  color: #0f172a;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 13px;
  line-height: 1.5;
}

.drawer-feedback-box {
  border-top: 1px dashed #cbd5e1;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fb-title {
  font-size: 12px;
  font-weight: 600;
  color: #0369a1;
  margin-bottom: 4px;
}

.fb-item.standard .fb-title {
  color: #b45309;
}

.fb-body {
  font-size: 12px;
  line-height: 1.6;
  color: #334155;
}

.edit-dialog-tip {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 10px;
}

/* ==================== 答题模式切换栏与手撕代码工作台样式 ==================== */
.answering-mode-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
  padding: 8px 14px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-bottom: 12px;
}

.mode-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #0369a1;
}

.live-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #0284c7;
  animation: pulse-dot 1.2s infinite;
}

.candidate-coding-console {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.coding-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.tool-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint-dropdown {
  margin-left: 4px;
}

.socratic-hint-banner {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 10px 14px;
}

.hint-banner-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.hint-tag {
  font-size: 12.5px;
  font-weight: 600;
  color: #b45309;
}

.hint-banner-body {
  font-size: 13px;
  line-height: 1.6;
  color: #78350f;
}

.coding-editor-wrapper {
  background: #0f172a;
  border-radius: 10px;
  border: 1px solid #1e293b;
  overflow: hidden;
}

.code-editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #1e293b;
  padding: 8px 14px;
  border-bottom: 1px solid #334155;
}

.file-name {
  color: #38bdf8;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-weight: 600;
  font-size: 13px;
}

.editor-sub {
  color: #94a3b8;
  font-size: 12px;
}

.code-textarea :deep(.el-textarea__inner) {
  background: #0b1120 !important;
  color: #f1f5f9 !important;
  font-family: 'JetBrains Mono', 'Fira Code', 'Courier New', monospace !important;
  font-size: 13.5px !important;
  line-height: 1.6 !important;
  border: none !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  padding: 12px 14px !important;
}

.live-sandbox-panel {
  background: #090d16;
  border-radius: 10px;
  border: 1px solid #1e293b;
  overflow: hidden;
}

.sandbox-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #131c2e;
  padding: 6px 12px;
  border-bottom: 1px solid #1e293b;
}

.panel-title {
  color: #cbd5e1;
  font-size: 12px;
  font-weight: 600;
}

.panel-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.meta-time {
  color: #38bdf8;
}

.meta-exit {
  color: #94a3b8;
}

.sandbox-panel-body {
  padding: 10px 14px;
  max-height: 150px;
  overflow-y: auto;
}

.testing-tip {
  color: #38bdf8;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.pulse-point {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #38bdf8;
  animation: pulse-dot 1.2s infinite;
}

.output-pre {
  margin: 0;
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 12.5px;
  line-height: 1.5;
  color: #a7f3d0;
  white-space: pre-wrap;
  word-break: break-all;
}

.output-pre code.error-out {
  color: #fca5a5;
}


/* ==================== 移动端轻量自适应媒体查询 ==================== */
@media (max-width: 992px) {
  .room-main-layout {
    flex-direction: column !important;
  }
  .interviewer-card, .candidate-card {
    width: 100% !important;
  }
  .interview-hud {
    flex-direction: column !important;
    gap: 8px !important;
    padding: 10px 14px !important;
  }
  .stage-hud {
    display: none !important;
  }
  .depth-hud {
    font-size: 11px !important;
  }
  .coding-toolbar {
    flex-direction: column !important;
    gap: 8px !important;
    align-items: flex-start !important;
  }
  .candidate-video-elem {
    max-height: 200px !important;
  }
}

</style>
