<template>
  <div class="tc-meeting-root" :class="{ 'fullscreen-mode': isFullScreen }" v-loading="loading">
    <!-- ==================== 1. 腾讯会议深色极简顶部条 ==================== -->
    <header class="tc-top-bar">
      <div class="tc-top-left">
        <div class="tc-brand-badge">
          <span class="tc-lock-icon">🔒</span>
          <span class="tc-brand-title">腾讯会议 · AI 专场</span>
        </div>
        <div class="tc-meeting-id-pill" title="点击复制会议邀请号" @click="copyMeetingLink">
          <span class="id-label">会议号：</span>
          <span class="id-num">952-841-306</span>
          <span class="copy-icon">📋</span>
        </div>
        <div class="tc-net-quality" title="端到端网络质量：优 (时延 12ms，丢包 0%)">
          <span class="sig-bar bar-1"></span>
          <span class="sig-bar bar-2"></span>
          <span class="sig-bar bar-3"></span>
          <span class="sig-bar bar-4"></span>
          <span class="sig-text">极佳</span>
        </div>
        <div class="tc-rec-badge">
          <span class="rec-blink-dot"></span>
          <span class="rec-label">REC {{ formatTimer(timerSeconds) }}</span>
        </div>
      </div>

      <div class="tc-top-center">
        <div class="tc-session-title">
          智问学伴 · 2026 届技术全真模拟面试间
        </div>
        <div class="tc-stage-indicator">
          <div class="stage-node" :class="{ active: currentStage >= 1, current: currentStage === 1 }">
            <span class="node-idx">1</span>
            <span class="node-txt">自我介绍</span>
          </div>
          <span class="node-arrow">&rarr;</span>
          <div class="stage-node" :class="{ active: currentStage >= 2, current: currentStage === 2 }">
            <span class="node-idx">2</span>
            <span class="node-txt">基础八股</span>
          </div>
          <span class="node-arrow">&rarr;</span>
          <div class="stage-node" :class="{ active: currentStage >= 3, current: currentStage === 3 }">
            <span class="node-idx">3</span>
            <span class="node-txt">项目深挖</span>
          </div>
          <span class="depth-tag">{{ getDepthText(currentDepth) }}</span>
        </div>
      </div>

      <div class="tc-top-right">
        <div class="tc-turn-counter">
          考题：<b>{{ currentTurnNum }}/{{ sessionData.totalTurns || 10 }}</b>
        </div>
        <div class="tc-view-switcher">
          <el-button-group size="small">
            <el-button
              :type="viewLayout === 'gallery' ? 'primary' : 'default'"
              class="tc-switch-btn"
              @click="viewLayout = 'gallery'"
            >
              ⊞ 画廊
            </el-button>
            <el-button
              :type="viewLayout === 'speaker' ? 'primary' : 'default'"
              class="tc-switch-btn"
              @click="viewLayout = 'speaker'"
            >
              👤 演讲
            </el-button>
          </el-button-group>
        </div>
        <el-button
          size="small"
          class="tc-tool-icon-btn"
          :title="isFullScreen ? '退出全屏' : '全屏体验'"
          @click="toggleFullScreen"
        >
          {{ isFullScreen ? '🗗 还原' : '⛶ 全屏' }}
        </el-button>
        <el-button
          size="small"
          class="tc-back-hall-btn"
          link
          @click="handleBack"
        >
          &times; 离开考场
        </el-button>
      </div>
    </header>

    <!-- ==================== 2. 腾讯会议核心舞台区 ==================== -->
    <main class="tc-main-stage" :class="['layout-' + viewLayout, { 'code-sharing-active': answerMode === 'code' }]">
      
      <!-- 模式 A：双人画廊视频网格 (普通口述问答模式) -->
      <div v-show="answerMode === 'voice'" class="tc-video-grid">
        
        <!-- 卡片 1: AI 主考官 (永雏塔菲 / 切换风格) -->
        <div
          class="tc-tile interviewer-tile"
          :class="{
            'active-speaker': isInterviewerSpeaking,
            'is-barge-interrupted': isBargeInActive,
            'view-focus': viewLayout === 'speaker'
          }"
        >
          <!-- 考官视窗头部状态条 -->
          <div class="tile-header">
            <div class="tile-status-tag">
              <span class="status-dot" :class="{ live: isInterviewerSpeaking }"></span>
              <span class="status-label">{{ interviewerRoleLabel }}</span>
            </div>

            <div class="tile-actions">
              <!-- 考官形象风格切换菜单 -->
              <el-dropdown trigger="click" @command="handleSwitchPersona">
                <span class="persona-switch-link">
                  {{ currentPersonaMeta.icon }} {{ currentPersonaMeta.name }} ▼
                </span>
                <template #dropdown>
                  <el-dropdown-menu class="tc-dropdown-dark">
                    <el-dropdown-item command="taffy">🎀 永雏塔菲 (二次元AI主考官)</el-dropdown-item>
                    <el-dropdown-item command="p7_architect">⚡ 阿里 P7 资深架构师</el-dropdown-item>
                    <el-dropdown-item command="bytedance_tech">💻 字节跳动技术专家</el-dropdown-item>
                    <el-dropdown-item command="gentle_hr">🤝 资深大厂 HRBP</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <!-- 状态气泡 -->
              <div class="activity-pill" :class="aiActivityState">
                <span v-if="isBargeInActive">⚡ 已插话打断考官</span>
                <span v-else-if="isInterviewerSpeaking">🗣️ 正在提问发音</span>
                <span v-else-if="aiActivityState === 'listening'">👂 专注倾听中</span>
                <span v-else-if="isGeminiThinking">🤔 Gemini 3.8 Flash 思考中</span>
                <span v-else>💡 考官就绪</span>
              </div>
            </div>
          </div>
          <!-- 考官形象主体渲染舞台 (永雏塔菲 Live2D / 交互矢量) -->
          <div class="avatar-canvas-box">
            <!-- 腾讯会议声纹波纹光晕 -->
            <div class="tc-voice-halo" :class="{ pulsing: isInterviewerSpeaking }">
              <div class="halo-layer layer-1"></div>
              <div class="halo-layer layer-2"></div>
            </div>

            <!-- ========== 形象 1: 永雏塔菲 (二次元AI主考官 / 真实Live2D动效立绘) ========== -->
            <div
              v-if="selectedPersonaStyle === 'taffy'"
              class="taffy-live-container"
              :class="[
                aiActivityState,
                { 'is-speaking': isInterviewerSpeaking, 'is-interrupted': isBargeInActive }
              ]"
            >
              <div class="taffy-model-wrapper">
                <img
                  :src="currentTaffySprite"
                  alt="永雏塔菲"
                  class="taffy-sprite-img"
                  @error="handleTaffyImgError"
                />
              </div>
            </div>

            <!-- ========== 形象 2: 阿里 P7 / 极客架构师 (备用风格) ========== -->
            <div
              v-else
              class="digital-human-avatar"
              :class="[aiActivityState, 'persona-' + selectedPersonaStyle]"
            >
              <svg viewBox="0 0 240 280" class="avatar-svg">
                <defs>
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
                </defs>
                <g class="avatar-body">
                  <path d="M 40 280 L 70 210 L 95 215 L 120 235 L 145 215 L 170 210 L 200 280 Z" fill="url(#suitGradP7)"/>
                  <polygon points="95,215 120,250 145,215" fill="#f8fafc"/>
                </g>
                <g class="avatar-head-group">
                  <rect x="106" y="170" width="28" height="42" rx="4" fill="#ed8936"/>
                  <path d="M 75 110 C 75 60, 165 60, 165 110 C 165 160, 145 185, 120 185 C 95 185, 75 160, 75 110 Z" fill="url(#skinGrad)"/>
                  <path d="M 70 100 C 65 50, 130 35, 170 65 C 175 90, 168 115, 160 115 C 155 75, 85 70, 75 105 Z" fill="#1a202c"/>
                  <ellipse cx="98" cy="115" rx="7" ry="5" fill="#1a202c"/>
                  <ellipse cx="142" cy="115" rx="7" ry="5" fill="#1a202c"/>
                  <path
                    v-if="!isInterviewerSpeaking"
                    d="M 106 156 Q 120 162 134 156"
                    stroke="#c53030"
                    stroke-width="3"
                    fill="none"
                  />
                  <path
                    v-else
                    d="M 105 154 Q 120 172 135 154 Z"
                    fill="#9b2c2c"
                  />
                </g>
              </svg>
            </div>
          </div>

          <!-- 腾讯会议经典左下角参会人名片胶囊 -->
          <div class="tc-participant-badge interviewer-badge">
            <span class="mic-status-icon" :class="{ 'is-speaking': isInterviewerSpeaking }">
              <span class="wave-line line-1"></span>
              <span class="wave-line line-2"></span>
              <span class="wave-line line-3"></span>
            </span>
            <span class="participant-name">{{ currentPersonaMeta.name }}</span>
            <span class="host-tag">主持人</span>
          </div>

          <!-- 考官语音控制快捷栏 (支持重新发音与暂停) -->
          <div class="tile-corner-tools">
            <el-button
              size="small"
              circle
              class="tc-mini-btn"
              :title="isInterviewerSpeaking ? '暂停朗读' : '重新朗读题目'"
              @click="toggleTtsCurrentQuestion"
            >
              {{ isInterviewerSpeaking ? '⏹️' : '🔊' }}
            </el-button>
          </div>
        </div>
        <!-- 卡片 2: 候选人我 (真实摄像头画面 / 实时声浪) -->
        <div
          class="tc-tile candidate-tile"
          :class="{
            'active-speaker': isRecording && audioEnergy > 10,
            'is-muted': !micActive
          }"
        >
          <!-- 候选人视窗头部 -->
          <div class="tile-header">
            <div class="tile-status-tag">
              <span class="status-dot candidate" :class="{ live: isRecording }"></span>
              <span class="status-label">候选人通道 · 1080P 30FPS</span>
            </div>
            <div class="tile-actions">
              <el-tag size="small" :type="cameraActive ? 'success' : 'info'" effect="dark">
                📷 {{ cameraActive ? '摄像头正常' : '已关' }}
              </el-tag>
              <el-tag size="small" :type="micActive ? 'success' : 'danger'" effect="dark">
                🎙️ {{ micActive ? '麦克风收音' : '静音' }}
              </el-tag>
            </div>
          </div>

          <!-- 视频画面视口 -->
          <div class="candidate-video-viewport">
            <video
              ref="candidateVideoRef"
              autoplay
              playsinline
              muted
              class="candidate-video-elem"
              :class="{ 'stream-hidden': !cameraActive }"
            ></video>

            <!-- 摄像头关闭时呈现腾讯会议头像剪影 -->
            <div v-if="!cameraActive" class="tc-camera-off-avatar">
              <div class="avatar-circle">👨‍💻</div>
              <div class="avatar-hint">摄像头已关闭 (音视频通道持续活跃)</div>
            </div>

            <!-- 实时声浪律动 HUD (腾讯会议动态音压) -->
            <div class="candidate-vu-hud">
              <span class="vu-tag" :class="{ 'barge-highlight': isBargeInActive }">
                {{ isBargeInActive ? '⚡ 插话打断中' : '🎙️ 音压：' }}
              </span>
              <div class="vu-bars">
                <div
                  v-for="i in 8"
                  :key="i"
                  class="vu-bar"
                  :class="{ 'barge-bar': isBargeInActive }"
                  :style="{ height: getDynamicBarHeight(i) + 'px' }"
                ></div>
              </div>
              <span v-if="isRecording && candidateWpm > 0" class="vu-wpm">
                {{ candidateWpm }} 字/分
              </span>
            </div>
          </div>

          <!-- 腾讯会议经典左下角参会人名片胶囊 -->
          <div class="tc-participant-badge candidate-badge">
            <span class="mic-status-icon" :class="{ 'is-speaking': isRecording && audioEnergy > 10, 'muted': !micActive }">
              <span v-if="!micActive" class="muted-slash">🚫</span>
              <template v-else>
                <span class="wave-line line-1"></span>
                <span class="wave-line line-2"></span>
                <span class="wave-line line-3"></span>
              </template>
            </span>
            <span class="participant-name">候选人 (我)</span>
            <span class="me-tag">我</span>
          </div>

          <!-- 快速控制 -->
          <div class="tile-corner-tools">
            <el-button
              size="small"
              circle
              class="tc-mini-btn"
              :type="cameraActive ? 'default' : 'danger'"
              :title="cameraActive ? '关闭摄像头' : '打开摄像头'"
              @click="toggleCamera"
            >
              📷
            </el-button>
            <el-button
              size="small"
              circle
              class="tc-mini-btn"
              :type="micActive ? 'default' : 'danger'"
              :title="micActive ? '静音' : '开启收音'"
              @click="toggleMic"
            >
              🎙️
            </el-button>
          </div>
        </div>

      </div>

      <!-- 模式 B：腾讯会议屏幕共享/代码实战模式 (当 answerMode === 'code' 时激活) -->
      <div v-show="answerMode === 'code'" class="tc-screen-share-stage">
        <div class="share-top-banner">
          <span class="share-icon">💻</span>
          <span class="share-text">李华 (我) 正在共享屏幕 · 算法代码手撕与沙箱实战</span>
          <el-button size="small" class="stop-share-btn" @click="answerMode = 'voice'">
            退出代码共享 ➔ 返回全屏对谈
          </el-button>
        </div>

        <div class="share-workspace-grid">
          <!-- 左侧：全屏代码编辑器与沙箱 -->
          <div class="share-code-pane">
            <div class="code-pane-toolbar">
              <div class="tool-left">
                <el-radio-group v-model="liveCodeLang" size="small" @change="handleLiveLangChange">
                  <el-radio-button label="java">☕ Java (JDK 17)</el-radio-button>
                  <el-radio-button label="python">🐍 Python 3</el-radio-button>
                  <el-radio-button label="javascript">⚡ JavaScript</el-radio-button>
                </el-radio-group>
                <el-dropdown trigger="click" @command="fetchSocraticHint" class="hint-dropdown">
                  <el-button size="small" type="warning" plain :loading="hintLoading">
                    💡 考官启发提示 ▼
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu class="tc-dropdown-dark">
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
                  🚀 提交代码 ➔
                </el-button>
              </div>
            </div>

            <!-- 苏格拉底启发卡片 -->
            <div v-if="currentHintText" class="socratic-hint-banner">
              <div class="hint-header">
                <span>💡 考官 Level {{ hintLevel }} 启发提示：</span>
                <el-button link type="info" size="small" @click="currentHintText = ''">&times;</el-button>
              </div>
              <div class="hint-body">{{ currentHintText }}</div>
            </div>

            <!-- 代码编辑区 -->
            <div class="share-editor-box">
              <el-input
                v-model="liveCode"
                type="textarea"
                :rows="14"
                class="code-textarea-dark"
                placeholder="在此编写算法解题代码..."
                spellcheck="false"
              />
            </div>

            <!-- 沙箱测试实时输出 -->
            <div class="live-sandbox-panel" v-if="liveSandboxResult || liveSandboxTesting">
              <div class="sandbox-header">
                <span class="title">🖥️ 沙箱运行输出：</span>
                <div class="meta" v-if="liveSandboxResult">
                  <el-tag :type="liveSandboxResult.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
                    {{ liveSandboxResult.status }}
                  </el-tag>
                  <span v-if="liveSandboxResult.executionTimeMs">⏱️ {{ liveSandboxResult.executionTimeMs }}ms</span>
                </div>
              </div>
              <pre class="sandbox-output"><code>{{ liveSandboxResult?.stdout || liveSandboxResult?.stderr || '沙箱运行测试中...' }}</code></pre>
            </div>
          </div>

          <!-- 右侧：腾讯会议画中画 (PIP) 考官视频观察小窗 -->
          <div class="share-pip-pane">
            <div class="pip-interviewer-card">
              <div class="pip-header">
                <span>主考官实时视线 · {{ currentPersonaMeta.name }}</span>
                <span class="pip-pulse"></span>
              </div>
              <div class="pip-avatar-view">
                <svg viewBox="0 0 260 300" class="pip-svg">
                  <circle cx="130" cy="130" r="55" fill="#ffb7d5"/>
                  <ellipse cx="130" cy="135" rx="40" ry="45" fill="#fff7f5"/>
                  <circle cx="115" cy="130" r="6" fill="#831843"/>
                  <circle cx="145" cy="130" r="6" fill="#831843"/>
                  <path d="M 124 150 Q 130 156 136 150" stroke="#e11d48" stroke-width="2" fill="none"/>
                </svg>
              </div>
              <div class="pip-question-bubble">
                “{{ currentTurn?.question || '请编写满足时空复杂度要求的解法...' }}”
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- ==================== 3. 腾讯会议中央考题看板与实时对谈流 ==================== -->
      <div class="tc-question-board" :class="{ 'barge-alert': isBargeInActive }">
        <div class="q-board-header">
          <div class="q-meta-left">
            <span class="q-stage-tag">第 {{ currentTurnNum }} 轮 · {{ getDepthText(currentDepth) }}</span>
            <span class="q-dim-tag">【考核核心】: {{ currentDimension }}</span>
          </div>
          <div class="q-meta-right">
            <span class="q-tts-tag">🎙️ {{ currentPersonaMeta.name }}</span>
            <el-button
              size="small"
              circle
              class="q-mini-btn"
              :title="isInterviewerSpeaking ? '暂停朗读' : '重新朗读题目'"
              @click="toggleTtsCurrentQuestion"
            >
              {{ isInterviewerSpeaking ? '⏹️' : '🔊' }}
            </el-button>
            <el-button
              v-if="isInterviewerSpeaking"
              size="small"
              type="warning"
              class="q-barge-btn"
              @click="triggerManualBargeIn"
            >
              ⚡ 插话打断
            </el-button>
          </div>
        </div>

        <!-- 考题正文（大字高对比度亮白） -->
        <div class="q-content-box">
          <span class="q-speaker-label">考官提问：</span>
          <span class="q-text">“{{ currentTurn?.question || '考题生成就绪中，请稍候...' }}”</span>
        </div>

        <!-- 候选人作答实时流 (作答中展示转写) -->
        <div v-if="isRecording || currentAnswer" class="q-answer-stream">
          <span class="a-speaker-label">候选人作答：</span>
          <span class="a-text">{{ currentAnswer || interimTranscript || '正在收音阐述作答中...' }}</span>
          <span v-if="isRecording" class="typing-cursor">▌</span>
        </div>
      </div>

    </main>

    <!-- ==================== 4. 腾讯会议经典底部控制栏 (Bottom Dock) ==================== -->
    <footer class="tc-bottom-dock">
      <div class="dock-content">
        
        <!-- 麦克风静音/开启 -->
        <div class="dock-btn-group" :class="{ active: micActive, muted: !micActive }" @click="toggleMic">
          <div class="dock-icon-circle mic-icon">
            <span class="icon-symbol">{{ micActive ? '🎙️' : '🔇' }}</span>
            <div v-if="micActive" class="mic-live-vu">
              <div class="vu-mini-bar" :style="{ height: Math.min(14, 3 + (audioEnergy / 100) * 11) + 'px' }"></div>
            </div>
          </div>
          <span class="dock-btn-label">{{ micActive ? '静音' : '解除静音' }}</span>
        </div>

        <!-- 摄像头开关 -->
        <div class="dock-btn-group" :class="{ active: cameraActive }" @click="toggleCamera">
          <div class="dock-icon-circle cam-icon">
            <span class="icon-symbol">{{ cameraActive ? '📷' : '🚫' }}</span>
          </div>
          <span class="dock-btn-label">{{ cameraActive ? '停止视频' : '开启视频' }}</span>
        </div>

        <div class="dock-separator"></div>

        <!-- 核心动作：全双工插话打断 (Barge-In) -->
        <div
          class="dock-btn-group barge-btn"
          :class="{ 'barge-available': isInterviewerSpeaking, 'barge-triggered': isBargeInActive }"
          title="当考官在发音提问时，点击或直接开口即可打断考官抢话"
          @click="triggerManualBargeIn"
        >
          <div class="dock-icon-circle barge-icon">
            <span class="icon-symbol">⚡</span>
          </div>
          <span class="dock-btn-label">{{ isBargeInActive ? '已插话打断' : '插话抢麦' }}</span>
        </div>

        <!-- 口述作答与手撕代码模式切换 -->
        <div
          class="dock-btn-group share-btn"
          :class="{ active: answerMode === 'code' }"
          @click="toggleAnswerMode"
        >
          <div class="dock-icon-circle">
            <span class="icon-symbol">{{ answerMode === 'code' ? '🎙️' : '💻' }}</span>
          </div>
          <span class="dock-btn-label">{{ answerMode === 'code' ? '返回口述' : '共享代码' }}</span>
        </div>

        <!-- 实时口述收音主控按钮 -->
        <div
          class="dock-btn-group record-main-btn"
          :class="{ recording: isRecording }"
          @click="toggleSpeechRecording"
        >
          <div class="dock-icon-circle record-icon">
            <span class="icon-symbol">{{ isRecording ? '⏹️' : '🔴' }}</span>
          </div>
          <span class="dock-btn-label">{{ isRecording ? '完成回答' : '开始作答' }}</span>
        </div>

        <!-- 实时字幕开关 -->
        <div
          class="dock-btn-group"
          :class="{ active: showSubtitles }"
          @click="showSubtitles = !showSubtitles"
        >
          <div class="dock-icon-circle">
            <span class="icon-symbol">📝</span>
          </div>
          <span class="dock-btn-label">{{ showSubtitles ? '隐藏字幕' : '实时字幕' }}</span>
        </div>

        <!-- 往轮记录与大厂标杆复盘抽屉 -->
        <div class="dock-btn-group" @click="historyDrawerVisible = true">
          <div class="dock-icon-circle badge-anchor">
            <span class="icon-symbol">📋</span>
            <span v-if="answeredTurnsCount > 0" class="dock-badge">{{ answeredTurnsCount }}</span>
          </div>
          <span class="dock-btn-label">考情纪要</span>
        </div>

        <!-- Gemini 3.8 Flash 引擎设置 -->
        <div class="dock-btn-group gemini-btn" @click="geminiDialogVisible = true">
          <div class="dock-icon-circle">
            <span class="icon-symbol">🤖</span>
          </div>
          <span class="dock-btn-label">AI 引擎</span>
        </div>

        <div class="dock-separator"></div>

        <!-- 腾讯会议经典红色离开/结束按钮 -->
        <div class="dock-btn-group leave-btn" @click="handleFinishInterview">
          <div class="dock-icon-circle end-icon">
            <span class="icon-symbol">🛑</span>
          </div>
          <span class="dock-btn-label">交卷离会</span>
        </div>

      </div>
    </footer>

    <!-- ==================== 5. 往轮问答与大厂标杆复盘抽屉 ==================== -->
    <el-drawer
      v-model="historyDrawerVisible"
      title="📋 模拟面试考情纪要与大厂标杆深度复盘"
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

    <!-- ==================== 6. AI 考官双引擎配置弹窗 (Gemini 3.8 + Fish Audio 塔菲克隆) ==================== -->
    <el-dialog
      v-model="geminiDialogVisible"
      title="⚙️ 考官底座大模型与永雏塔菲原声克隆配置"
      width="620px"
      append-to-body
      class="tc-dialog-dark"
    >
      <el-tabs v-model="activeEngineTab" class="tc-engine-tabs">
        <!-- Tab 1: 永雏塔菲 TTS 原声克隆 -->
        <el-tab-pane label="🎙️ 永雏塔菲原声 (Fish Audio)" name="tts">
          <div class="gemini-config-form">
            <div class="tts-mode-selector">
              <el-radio-group v-model="ttsConfig.engine" size="small">
                <el-radio-button label="fish_audio">🐟 Fish Audio 云端流式克隆 (永雏塔菲原声)</el-radio-button>
                <el-radio-button label="browser">⚡ Edge-TTS / 浏览器极速合成 (纯本地兜底)</el-radio-button>
              </el-radio-group>
            </div>

            <div v-if="ttsConfig.engine === 'fish_audio'">
              <el-alert
                title="已挂载 Fish Audio 社区高赞“永雏塔菲”公开音色模型 (163+ Likes)。无需 GPU 算力，云端流式首包时延约 200ms，毫秒级全双工抢麦打断。"
                type="info"
                :closable="false"
                show-icon
                style="margin-bottom: 16px;"
              />
              <el-form label-width="120px" size="default">
                <el-form-item label="TTS 端点：">
                  <el-input v-model="ttsConfig.fishEndpoint" placeholder="https://api.fish.audio/v1/tts" />
                </el-form-item>
                <el-form-item label="Fish API Key：">
                  <el-input
                    v-model="ttsConfig.fishApiKey"
                    type="password"
                    show-password
                    placeholder="输入 Fish Audio API Key (留空将自动回退二次元少女音)"
                  />
                  <div class="form-item-tip">
                    <span>首次使用？</span>
                    <a href="https://fish.audio/zh-CN/go-api/" target="_blank" rel="noopener noreferrer" style="color: #38bdf8; text-decoration: underline; margin-left: 4px;">
                      点此免费领取 Fish Audio API Key (赠送充沛额度)
                    </a>
                  </div>
                </el-form-item>
                <el-form-item label="塔菲模型 ID：">
                  <el-input v-model="ttsConfig.fishReferenceId" placeholder="55b28b196e1c4fff9a55cd32a46eff25" />
                  <div class="form-item-tip">
                    预置社区认证“永雏塔菲”公开音色；亦可替换为您在 fish.audio 上自定义的模型 ID。
                  </div>
                </el-form-item>
                <el-form-item label="流式延迟：">
                  <el-radio-group v-model="ttsConfig.fishLatency" size="small">
                    <el-radio label="balanced">均衡低时延 (balanced - 推荐)</el-radio>
                    <el-radio label="normal">高保真音质 (normal)</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="塔菲声线试听：">
                  <el-button type="success" plain :loading="testingTts" @click="testFishAudioConnectivity">
                    ⚡ 试听永雏塔菲原声合成
                  </el-button>
                  <span v-if="ttsTestResult" class="gemini-test-msg" :class="ttsTestResult.status">
                    {{ ttsTestResult.msg }}
                  </span>
                </el-form-item>
              </el-form>
            </div>

            <div v-else>
              <el-alert
                title="纯本地浏览器语音合成（优先调用微软晓晓/晓伊二次元高音调），完全离线、0 网络依赖、0 算力占用、毫秒级发音。"
                type="success"
                :closable="false"
                show-icon
                style="margin-bottom: 16px;"
              />
              <el-form label-width="120px" size="default">
                <el-form-item label="当前合成音色：">
                  <el-input :model-value="ttsVoiceLabel" disabled />
                </el-form-item>
                <el-form-item label="二次元升调音：">
                  <el-button type="primary" plain @click="previewXiaoxiaoVoice">
                    🔊 试听浏览器二次元升调语音
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 2: Gemini 3.8 Flash 考官大脑 -->
        <el-tab-pane label="🤖 考官大脑 (Gemini 3.8 Flash)" name="llm">
          <div class="gemini-config-form">
            <el-alert
              title="已直连本地代理通道 (http://127.0.0.1:8317/v1)，支持全双工低时延实时对话"
              type="success"
              :closable="false"
              show-icon
              style="margin-bottom: 16px;"
            />
            <el-form label-width="110px" size="default">
              <el-form-item label="接口端点：">
                <el-input v-model="geminiConfig.endpoint" placeholder="http://127.0.0.1:8317/v1" />
              </el-form-item>
              <el-form-item label="模型代号：">
                <el-input v-model="geminiConfig.model" placeholder="gemini-3.8-flash-high" />
              </el-form-item>
              <el-form-item label="访问令牌：">
                <el-input v-model="geminiConfig.apiKey" type="password" show-password placeholder="123456" />
              </el-form-item>
              <el-form-item label="连通测试：">
                <el-button type="primary" plain :loading="testingGemini" @click="testGeminiConnectivity">
                  ⚡ 立即测试连通性
                </el-button>
                <span v-if="geminiTestResult" class="gemini-test-msg" :class="geminiTestResult.status">
                  {{ geminiTestResult.msg }}
                </span>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="geminiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEngineConfig">保存配置</el-button>
      </template>
    </el-dialog>

    <!-- 文本微调弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="✏️ 口述作答文本微调（校准专有名词）"
      width="540px"
      append-to-body
    >
      <div class="edit-dialog-tip">
        提示：若 Netty、B+Tree、CAS 等专业英文缩写有微小转写误差，可在此微调校准后直接提交。
      </div>
      <el-input
        v-model="currentAnswer"
        type="textarea"
        :rows="5"
        resize="none"
        placeholder="微调您的作答内容..."
      />
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="editDialogVisible = false">确定校准</el-button>
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

// ==================== 音视频考场与全双工打断逻辑 ====================
const timerSeconds = ref(0)
let timerInterval = null
let virtualAudioInterval = null

const candidateVideoRef = ref(null)
const cameraActive = ref(true)
const micActive = ref(true)
let candidateStream = null
let candidateAudioCtx = null
let candidateAnalyser = null
let animFrameId = null
const audioEnergy = ref(0)
const fftFrequencyBars = ref([4, 4, 4, 4, 4, 4, 4, 4])
const isBargeInActive = ref(false)
const candidateWpm = ref(0)
let consecutiveSpeechFrames = 0
let bargeInResetTimeout = null
let speechStartTime = 0

const isInterviewerSpeaking = ref(false)
let currentUtterance = null
let ttsHeartbeatInterval = null

const isRecording = ref(false)
let speechRecognitionInstance = null
const interimTranscript = ref('')


const hasNotifiedMicOn = ref(false)

const showNotice = (msg, type = 'info') => {
  ElMessage({
    message: msg,
    type,
    grouping: true,
    duration: 2200
  })
}



// ==================== 腾讯会议 & 永雏塔菲 & Gemini 3.8 Flash 配置 ====================
const selectedPersonaStyle = ref('taffy')
const taffyMouthState = ref(0) // 0: idle, 1: half, 2: full
let taffyMouthInterval = null

const startTaffyLipSync = () => {
  stopTaffyLipSync()
  const sequence = [1, 2, 1, 0, 2, 1, 0, 1]
  let idx = 0
  taffyMouthInterval = setInterval(() => {
    if (!isInterviewerSpeaking.value) {
      taffyMouthState.value = 0
      stopTaffyLipSync()
      return
    }
    taffyMouthState.value = sequence[idx % sequence.length]
    idx++
  }, 130)
}

const stopTaffyLipSync = () => {
  if (taffyMouthInterval) {
    clearInterval(taffyMouthInterval)
    taffyMouthInterval = null
  }
  taffyMouthState.value = 0
}


const handleTaffyImgError = (e) => {
  console.warn('塔菲立绘加载兜底:', e)
  if (e.target && !e.target.dataset.retried) {
    e.target.dataset.retried = '1'
    e.target.src = '/avatar/taffy/taffy_idle.png'
  }
}

const currentTaffySprite = computed(() => {
  if (isBargeInActive.value) {
    return '/avatar/taffy/taffy_active.png'
  }
  if (isInterviewerSpeaking.value) {
    if (taffyMouthState.value === 2) return '/avatar/taffy/taffy_speak_full.png'
    if (taffyMouthState.value === 1) return '/avatar/taffy/taffy_speak_half.png'
    return '/avatar/taffy/taffy_idle.png'
  }
  if (submittingAnswer.value) {
    return '/avatar/taffy/taffy_cheer.png'
  }
  return '/avatar/taffy/taffy_idle.png'
})
 // 'taffy' | 'p7_architect' | 'bytedance_tech' | 'gentle_hr'
const viewLayout = ref('gallery') // 'gallery' | 'speaker'
const isFullScreen = ref(false)
const showSubtitles = ref(true)

const geminiDialogVisible = ref(false)
const activeEngineTab = ref('tts')
const geminiConfig = reactive({
  endpoint: 'http://127.0.0.1:8317/v1',
  model: 'gemini-3.8-flash-high',
  apiKey: '123456'
})
const isGeminiThinking = ref(false)
const testingGemini = ref(false)
const geminiTestResult = ref(null)

const ttsConfig = reactive({
  engine: 'fish_audio', // 'fish_audio' | 'browser'
  fishEndpoint: 'https://api.fish.audio/v1/tts',
  fishApiKey: '',
  fishReferenceId: '55b28b196e1c4fff9a55cd32a46eff25', // 永雏塔菲标准模型ID
  fishLatency: 'balanced'
})
const testingTts = ref(false)
const ttsTestResult = ref(null)
let currentAudioElement = null
let currentAudioAbortController = null

const copyMeetingLink = () => {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText('952-841-306')
  }
  ElMessage.success('已复制腾讯会议号：952-841-306')
}

const toggleFullScreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen().then(() => {
      isFullScreen.value = true
    }).catch(() => {
      isFullScreen.value = !isFullScreen.value
    })
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen().then(() => {
        isFullScreen.value = false
      })
    }
  }
}

const handleSwitchPersona = (style) => {
  selectedPersonaStyle.value = style
  sessionData.value.interviewerStyle = style
  ElMessage.success(`已切换主考官形象：${currentPersonaMeta.value.name}`)
}

const triggerManualBargeIn = () => {
  if (isInterviewerSpeaking.value) {
    handleBargeInInterrupt()
  } else {
    ElMessage.info('考官当前未发音，您可直接开始作答')
    if (!isRecording.value) {
      startSpeechRecording()
    }
  }
}

const toggleAnswerMode = () => {
  answerMode.value = answerMode.value === 'code' ? 'voice' : 'code'
  ElMessage.info(answerMode.value === 'code' ? '已开启屏幕共享 · 算法代码手撕模式' : '已返回双人全屏对谈模式')
}

const toggleSpeechRecording = () => {
  if (isRecording.value) {
    stopAndSubmitSpeechAnswer()
  } else {
    startSpeechRecording()
  }
}

const testGeminiConnectivity = async () => {
  testingGemini.value = true
  geminiTestResult.value = null
  const startTime = Date.now()
  try {
    const url = (geminiConfig.endpoint.endsWith('/') ? geminiConfig.endpoint.slice(0, -1) : geminiConfig.endpoint) + '/chat/completions'
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${geminiConfig.apiKey}`
      },
      body: JSON.stringify({
        model: geminiConfig.model,
        messages: [{ role: 'user', content: '测试连接' }],
        max_tokens: 15
      })
    })
    const data = await resp.json()
    const elapsed = Date.now() - startTime
    if (resp.ok && data.choices && data.choices[0]) {
      geminiTestResult.value = {
        status: 'success',
        msg: `✅ 连通极佳！时延 ${elapsed}ms · 回复: “${data.choices[0].message.content.trim()}”`
      }
    } else {
      geminiTestResult.value = {
        status: 'error',
        msg: `❌ 调用异常: ${data.error?.message || resp.statusText}`
      }
    }
  } catch (err) {
    geminiTestResult.value = {
      status: 'error',
      msg: `❌ 网络连接失败: ${err.message}`
    }
  } finally {
    testingGemini.value = false
  }
}

const saveEngineConfig = () => {
  localStorage.setItem('interview_gemini_config', JSON.stringify(geminiConfig))
  localStorage.setItem('interview_tts_config', JSON.stringify(ttsConfig))
  geminiDialogVisible.value = false
  ElMessage.success('考官大脑与永雏塔菲原声配置已保存生效')
}
const saveGeminiConfig = saveEngineConfig

const testFishAudioConnectivity = async () => {
  testingTts.value = true
  ttsTestResult.value = null
  const sampleText = '你好呀！我是你的主考官永雏塔菲，请做好准备，我们即将进入技术深挖。'

  if (!ttsConfig.fishApiKey || !ttsConfig.fishApiKey.trim()) {
    ttsTestResult.value = {
      status: 'warning',
      msg: '⚠️ 未填入 Fish Audio Key，已为您演示浏览器二次元升调少女音'
    }
    testingTts.value = false
    previewXiaoxiaoVoice()
    return
  }

  const startTime = Date.now()
  try {
    const endpoint = ttsConfig.fishEndpoint || 'https://api.fish.audio/v1/tts'
    const resp = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${ttsConfig.fishApiKey.trim()}`
      },
      body: JSON.stringify({
        text: sampleText,
        reference_id: ttsConfig.fishReferenceId || '55b28b196e1c4fff9a55cd32a46eff25',
        format: 'mp3',
        latency: ttsConfig.fishLatency || 'balanced'
      })
    })

    if (!resp.ok) {
      const errText = await resp.text()
      throw new Error(`HTTP ${resp.status}: ${errText.slice(0, 100)}`)
    }

    const elapsed = Date.now() - startTime
    const blob = await resp.blob()
    const url = URL.createObjectURL(blob)
    stopAllTts()
    const audio = new Audio(url)
    currentAudioElement = audio
    audio.onended = () => {
      isInterviewerSpeaking.value = false
      URL.revokeObjectURL(url)
      currentAudioElement = null
    }
    audio.onerror = () => {
      isInterviewerSpeaking.value = false
      URL.revokeObjectURL(url)
      currentAudioElement = null
    }
    await audio.play()
    isInterviewerSpeaking.value = true

    ttsTestResult.value = {
      status: 'success',
      msg: `✅ 塔菲音色试听成功！云端时延 ${elapsed}ms · 正在播放原声`
    }
  } catch (err) {
    ttsTestResult.value = {
      status: 'error',
      msg: `❌ 合成失败: ${err.message}`
    }
  } finally {
    testingTts.value = false
  }
}

const interviewerRoleLabel = computed(() => {
  return `主考官连线 · ${currentPersonaMeta.value.name}`
})

const currentPersonaMeta = computed(() => {
  const style = selectedPersonaStyle.value || sessionData.value?.interviewerStyle || 'taffy'
  if (style === 'taffy') {
    return { icon: '🎀', name: '永雏塔菲 (二次元AI主考官)' }
  }
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

// ==================== 手撕代码与沙箱评测逻辑 ====================
const answerMode = ref('voice') // 'voice' | 'code'

const liveCodeLang = ref('java')
const liveSandboxTesting = ref(false)
const liveSandboxResult = ref(null)
const hintLoading = ref(false)
const currentHintText = ref('')
const hintLevel = ref(1)

const interviewCodeTemplates = {
  java: `public class Solution {\n    public static void main(String[] args) {\n        // 编写解题逻辑与测试用例\n        System.out.println("Solution initialized");\n    }\n}`,
  python: `def solve():\n    # 编写解题逻辑与测试用例\n    print("Solution initialized")\n\nif __name__ == "__main__":\n    solve()`,
  javascript: `function solve() {\n    // 编写解题逻辑与测试用例\n    console.log("Solution initialized");\n}\nsolve();`
}

const liveCode = ref(interviewCodeTemplates.java)

const handleLiveLangChange = (lang) => {
  if (interviewCodeTemplates[lang]) {
    liveCode.value = interviewCodeTemplates[lang]
  }
}

const resetLiveCode = () => {
  const tpl = interviewCodeTemplates[liveCodeLang.value]
  if (tpl) liveCode.value = tpl
  ElMessage.info('已重置为基础代码骨架模板')
}

const fetchSocraticHint = async (cmd) => {
  const lvl = Number(cmd) || 1
  hintLevel.value = lvl
  hintLoading.value = true
  try {
    const res = await getInterviewHint({
      sessionId: sessionId || sessionData.value?.id,
      turnNum: currentTurnNum.value,
      level: lvl
    })
    const payload = res.data || res
    currentHintText.value = payload.hint || payload.content || '考官建议：先分析题目边界用例，推演核心数据结构（如哈希表/双指针）。'
    ElMessage.success(`已获取考官 Level ${lvl} 启发提示`)
  } catch (err) {
    console.warn('获取启发提示异常，降级显示内置启发:', err)
    if (lvl === 1) {
      currentHintText.value = '【思路选型启发】：考虑时空平衡，若要求 O(n) 时间，可权衡是否能用空间换时间（例如 HashMap 缓存中间状态）。'
    } else if (lvl === 2) {
      currentHintText.value = '【边界防御提示】：注意防御空指针、越界下标以及极大/极小整数溢出情形。'
    } else {
      currentHintText.value = '【高阶调优启发】：推演是否能原地修改指针实现 O(1) 额外空间复杂度？'
    }
  } finally {
    hintLoading.value = false
  }
}

const testRunSandbox = async () => {
  if (!liveCode.value.trim()) {
    ElMessage.warning('代码内容不能为空')
    return
  }
  liveSandboxTesting.value = true
  liveSandboxResult.value = null
  try {
    const res = await runInterviewSandbox({
      language: liveCodeLang.value,
      code: liveCode.value
    })
    liveSandboxResult.value = res.data || res
    ElMessage.success('沙箱测试用例执行完毕')
  } catch (err) {
    console.warn('沙箱运行报错:', err)
    liveSandboxResult.value = {
      status: 'RUNTIME_ERROR',
      stdout: '',
      stderr: '沙箱隔离容器执行超时或异常: ' + (err.message || '系统限制'),
      executionTimeMs: 120
    }
  } finally {
    liveSandboxTesting.value = false
  }
}

const handleInterviewCodeSubmit = async () => {
  if (!liveCode.value.trim()) {
    ElMessage.warning('请先编写算法代码后再提交')
    return
  }
  submittingAnswer.value = true
  try {
    const res = await submitInterviewCode({
      sessionId: sessionId || sessionData.value?.id,
      turnId: currentTurn.value?.id,
      language: liveCodeLang.value,
      code: liveCode.value
    })
    ElMessage.success('🚀 代码已通过沙箱深度审计并成功提交！')
    currentAnswer.value = `【算法手撕代码 (${liveCodeLang.value})】：\n` + liveCode.value
    await handleSubmitAnswer()
    answerMode.value = 'voice'
  } catch (err) {
    ElMessage.error(err.message || '代码提交评测失败')
  } finally {
    submittingAnswer.value = false
  }
}

const currentTurn = computed(() => {
  const turns = sessionData.value.turns || []
  if (turns.length === 0) return null
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
  if (num <= 1) return 1
  if (num <= Math.max(2, Math.floor(total / 2))) return 2
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

const aiActivityState = computed(() => {
  if (isBargeInActive.value) return 'interrupted'
  if (isInterviewerSpeaking.value) return 'speaking'
  if (isRecording.value) return 'listening'
  return 'idle'
})

const getPersonaMeta = computed(() => {
  return currentPersonaMeta.value
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
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const formatTime = (timeStr) => {
  if (!timeStr) return '--:--'
  return timeStr.replace('T', ' ').substring(11, 16)
}

const goToReport = () => {
  const targetId = sessionId || (sessionData.value && sessionData.value.id)
  if (!targetId) {
    ElMessage.warning('面试场次ID不存在，无法进入报告')
    return
  }
  router.push({ name: 'interviewReport', params: { id: targetId } })
}

const handleBack = () => {
  router.push({ name: 'interviewIndex' })
}

// 动态全双工打断逻辑 (Barge-In)
const handleBargeInInterrupt = () => {
  if (!isInterviewerSpeaking.value) return
  console.log('[Barge-In] 候选人主动发声打断考官')
  stopAllTts()
  isBargeInActive.value = true
  ElMessage.warning('⚡ 已检测到您的发言，考官已让出麦克风')

  if (bargeInResetTimeout) clearTimeout(bargeInResetTimeout)
  bargeInResetTimeout = setTimeout(() => {
    isBargeInActive.value = false
  }, 2500)

  if (!isRecording.value) {
    startSpeechRecording()
  }
}

const getDynamicBarHeight = (index) => {
  if (!micActive.value || (!isRecording.value && audioEnergy.value < 5)) {
    return 4
  }
  if (fftFrequencyBars.value && fftFrequencyBars.value[index - 1] > 4) {
    return fftFrequencyBars.value[index - 1]
  }
  const factor = (Math.sin(Date.now() / 150 + index) + 1) / 2
  const base = Math.max(4, (audioEnergy.value / 100) * 22 * factor)
  return Math.round(base)
}

// 初始化候选人音视频流与能量监听
const initCandidateMedia = async () => {
  const isSimulation = sessionStorage.getItem('interview_camera_simulation') === '1'
  const hasHardwareApi = Boolean(navigator.mediaDevices && navigator.mediaDevices.getUserMedia)

  const startVirtualStream = () => {
    try {
      const stream = createVirtualCameraStream('候选人（我）')
      candidateStream = stream
      cameraActive.value = true
      micActive.value = true
      if (candidateVideoRef.value) {
        candidateVideoRef.value.srcObject = stream
      }
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

        const step = Math.max(1, Math.floor(bufferLength / 8))
        for (let idx = 0; idx < 8; idx++) {
          fftFrequencyBars.value[idx] = Math.max(4, Math.min(24, Math.round((dataArray[idx * step] / 255) * 24)))
        }

        // 全双工智能打断检测
        if (isInterviewerSpeaking.value && audioEnergy.value > 16) {
          consecutiveSpeechFrames++
          if (consecutiveSpeechFrames >= 3) {
            handleBargeInInterrupt()
          }
        } else {
          consecutiveSpeechFrames = 0
        }

        animFrameId = requestAnimationFrame(trackVolume)
      }
      trackVolume()
    } catch (e) {
      console.warn('候选人音频分析器初始化忽略:', e)
    }
  } catch (err) {
    console.warn('获取候选人真实摄像头失败，降级为虚拟全息摄像头:', err)
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
    showNotice(micActive.value ? '麦克风已开启' : '🔇 麦克风已静音', 'info')
  }
}

// ==================== 语音合成 TTS 引擎 (双引擎：Fish Audio 塔菲原声 + Edge-TTS 兜底) ====================
const ttsVoiceLabel = ref('自然拟人语音 (载入中...)')
const isXiaoxiaoReady = ref(false)
let currentXiaoxiaoVoice = null

const stopAllTts = () => {
  if (currentAudioAbortController) {
    try {
      currentAudioAbortController.abort()
    } catch (e) {}
    currentAudioAbortController = null
  }
  if (currentAudioElement) {
    try {
      currentAudioElement.pause()
      currentAudioElement.currentTime = 0
    } catch (e) {}
    currentAudioElement = null
  }
  if ('speechSynthesis' in window && window.speechSynthesis.speaking) {
    window.speechSynthesis.cancel()
  }
  isInterviewerSpeaking.value = false
  if (ttsHeartbeatInterval) {
    clearInterval(ttsHeartbeatInterval)
    ttsHeartbeatInterval = null
  }
}

const initXiaoxiaoVoice = () => {
  if (!('speechSynthesis' in window)) {
    ttsVoiceLabel.value = '未支持语音合成'
    return
  }

  const findBestVoice = () => {
    const voices = window.speechSynthesis.getVoices()
    if (!voices || voices.length === 0) return false

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
      ttsVoiceLabel.value = `${naturalZh.name} (自然中文)`
      return true
    }

    const fallbackZh = voices.find(v => v.lang && (v.lang.includes('zh') || v.lang.includes('CN')))
    if (fallbackZh) {
      currentXiaoxiaoVoice = fallbackZh
      isXiaoxiaoReady.value = true
      ttsVoiceLabel.value = `${fallbackZh.name} (系统中文)`
      return true
    }

    return false
  }

  if (!findBestVoice()) {
    window.speechSynthesis.onvoiceschanged = () => {
      findBestVoice()
    }
  }
}

const speakBrowserSpeech = (text) => {
  if (!('speechSynthesis' in window)) return
  if (!text || !text.trim()) return

  if (window.speechSynthesis.speaking) {
    window.speechSynthesis.cancel()
  }

  const utterance = new SpeechSynthesisUtterance(text)
  if (currentXiaoxiaoVoice) {
    utterance.voice = currentXiaoxiaoVoice
  }
  utterance.lang = 'zh-CN'
  utterance.rate = 1.05
  utterance.pitch = selectedPersonaStyle.value === 'taffy' ? 1.25 : 1.0

  utterance.onstart = () => {
    isInterviewerSpeaking.value = true
    if (ttsHeartbeatInterval) clearInterval(ttsHeartbeatInterval)
    ttsHeartbeatInterval = setInterval(() => {
      if (window.speechSynthesis && window.speechSynthesis.speaking) {
        window.speechSynthesis.pause()
        window.speechSynthesis.resume()
      } else {
        clearInterval(ttsHeartbeatInterval)
      }
    }, 10000)
  }

  utterance.onend = () => {
    isInterviewerSpeaking.value = false
    if (ttsHeartbeatInterval) clearInterval(ttsHeartbeatInterval)
  }

  utterance.onerror = (e) => {
    console.warn('TTS 发音中断/错误:', e)
    isInterviewerSpeaking.value = false
    if (ttsHeartbeatInterval) clearInterval(ttsHeartbeatInterval)
  }

  currentUtterance = utterance
  window.speechSynthesis.speak(utterance)
}

const speakWithFishAudio = async (text) => {
  stopAllTts()
  if (!text || !text.trim()) return

  if (!ttsConfig.fishApiKey || !ttsConfig.fishApiKey.trim()) {
    ElMessage.info('💡 未配置 Fish Audio Key，自动回退至浏览器二次元少女音')
    speakBrowserSpeech(text)
    return
  }

  currentAudioAbortController = new AbortController()

  try {
    const endpoint = ttsConfig.fishEndpoint || 'https://api.fish.audio/v1/tts'
    const resp = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${ttsConfig.fishApiKey.trim()}`
      },
      body: JSON.stringify({
        text: text.trim(),
        reference_id: ttsConfig.fishReferenceId || '55b28b196e1c4fff9a55cd32a46eff25',
        format: 'mp3',
        latency: ttsConfig.fishLatency || 'balanced'
      }),
      signal: currentAudioAbortController.signal
    })

    if (!resp.ok) {
      const errText = await resp.text()
      throw new Error(`Fish Audio 状态 ${resp.status}: ${errText.slice(0, 80)}`)
    }

    const blob = await resp.blob()
    const audioUrl = URL.createObjectURL(blob)
    const audio = new Audio(audioUrl)
    currentAudioElement = audio

    audio.onplay = () => {
      isInterviewerSpeaking.value = true
    }

    audio.onended = () => {
      isInterviewerSpeaking.value = false
      URL.revokeObjectURL(audioUrl)
      currentAudioElement = null
    }

    audio.onerror = (e) => {
      console.warn('Fish Audio 播放异常:', e)
      isInterviewerSpeaking.value = false
      URL.revokeObjectURL(audioUrl)
      currentAudioElement = null
      speakBrowserSpeech(text)
    }

    await audio.play()
  } catch (err) {
    if (err.name === 'AbortError') {
      console.log('[Fish Audio] 候选人主动发声，考官已抢麦打断')
      return
    }
    console.warn('Fish Audio 合成失败，回退至浏览器语音:', err)
    ElMessage.warning('Fish Audio 调用异常，已自动回退到浏览器二次元音色')
    isInterviewerSpeaking.value = false
    speakBrowserSpeech(text)
  }
}

const speakQuestionText = (text) => {
  if (ttsConfig.engine === 'fish_audio') {
    speakWithFishAudio(text)
  } else {
    speakBrowserSpeech(text)
  }
}

const toggleTtsCurrentQuestion = () => {
  if (isInterviewerSpeaking.value) {
    stopAllTts()
    ElMessage.info('已暂停考官朗读')
  } else {
    const qText = currentTurn.value?.question
    if (qText) {
      speakQuestionText(qText)
    }
  }
}

const previewXiaoxiaoVoice = () => {
  stopAllTts()
  speakQuestionText('你好呀！我是你的主考官永雏塔菲，请做好准备，我们即将进入技术深挖。')
}

// ==================== 语音识别 ASR 引擎 ====================
const startSpeechRecording = () => {
  if (isInterviewerSpeaking.value) {
    window.speechSynthesis.cancel()
    isInterviewerSpeaking.value = false
  }

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    showNotice('当前浏览器暂未启用 Web Speech API，建议使用 Chrome/Edge 浏览器参与全真口述面试', 'warning')
    return
  }

  if (!hasNotifiedMicOn.value) {
    showNotice('🎙️ 麦克风已开启，请开口口述作答', 'success')
    hasNotifiedMicOn.value = true
  }

  try {
    speechStartTime = Date.now()
    const recognition = new SpeechRecognition()
    recognition.lang = 'zh-CN'
    recognition.continuous = true
    recognition.interimResults = true

    recognition.onstart = () => {
      isRecording.value = true
      interimTranscript.value = ''
    }

    recognition.onresult = (event) => {
      let interim = ''
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) {
          const finalStr = event.results[i][0].transcript
          currentAnswer.value = (currentAnswer.value + ' ' + finalStr).trim()
          const elapsedMin = (Date.now() - speechStartTime) / 60000
          if (elapsedMin > 0.05) {
            candidateWpm.value = Math.round(currentAnswer.value.length / elapsedMin)
          }
        } else {
          interim += event.results[i][0].transcript
        }
      }
      interimTranscript.value = interim
    }

    recognition.onerror = (e) => {
      console.warn('语音识别事件:', e.error)
      if (e.error === 'not-allowed') {
        ElMessage.error('麦克风权限未授予，无法进行语音口述')
        isRecording.value = false
      }
    }

    recognition.onend = () => {
      if (isRecording.value) {
        try {
          recognition.start()
        } catch (err) {
          isRecording.value = false
        }
      }
    }

    recognition.start()
    speechRecognitionInstance = recognition
  } catch (e) {
    console.error('启动语音识别失败:', e)
    ElMessage.error('启动语音识别失败，请检查浏览器权限')
    isRecording.value = false
  }
}

const stopSpeechRecordingOnly = () => {
  isRecording.value = false
  hasNotifiedMicOn.value = false
  if (speechRecognitionInstance) {
    try {
      speechRecognitionInstance.stop()
    } catch (e) {}
    speechRecognitionInstance = null
  }
  if (interimTranscript.value.trim()) {
    currentAnswer.value = (currentAnswer.value + ' ' + interimTranscript.value.trim()).trim()
    interimTranscript.value = ''
  }
  showNotice('已暂停麦克风收音', 'info')
}

const handleClearSpeechAnswer = () => {
  currentAnswer.value = ''
  interimTranscript.value = ''
  candidateWpm.value = 0
  ElMessage.info('已清空当前作答草稿')
}

const handleSubmitAnswer = async () => {
  const text = currentAnswer.value.trim()
  if (!text) {
    ElMessage.warning('请先口述或编写您的回答')
    return
  }

  stopSpeechRecordingOnly()
  submittingAnswer.value = true

  try {
    const res = await submitInterviewAnswer({
      sessionId: sessionId || sessionData.value?.id,
      turnId: currentTurn.value?.id,
      answer: text
    })

    ElMessage.success('作答提交成功，考官正在综合评定并组织下一轮考题...')
    currentAnswer.value = ''
    interimTranscript.value = ''
    candidateWpm.value = 0

    await loadSession()

    nextTick(() => {
      if (currentTurn.value?.question) {
        speakQuestionText(currentTurn.value.question)
      }
    })
  } catch (err) {
    ElMessage.error(err.message || '答题提交失败')
  } finally {
    submittingAnswer.value = false
  }
}

const stopAndSubmitSpeechAnswer = () => {
  handleSubmitAnswer()
}

// ==================== 场次数据加载与交卷 ====================
const loadSession = async () => {
  try {
    const res = await getInterviewDetail(sessionId)
    sessionData.value = res.data || res

    if (sessionData.value.durationSeconds) {
      timerSeconds.value = sessionData.value.durationSeconds
    }

    // 优先保持永雏塔菲（二次元AI主考官）为当前连线形象
    selectedPersonaStyle.value = 'taffy'

    if (sessionData.value.status === 2 || sessionData.value.status === 3) {
      if (timerInterval) clearInterval(timerInterval)
      if (isRecording.value) stopSpeechRecordingOnly()
      if (isInterviewerSpeaking.value) window.speechSynthesis.cancel()
    }
  } catch (err) {
    ElMessage.error('加载面试场次数据失败: ' + (err.message || '网络异常'))
  } finally {
    loading.value = false
  }
}

const handleFinishInterview = () => {
  const turns = sessionData.value.turns || []
  const hasAnyAnswer = turns.some(t => Boolean(t.userAnswer && t.userAnswer.trim()))
  const confirmMsg = hasAnyAnswer
    ? '您确定要交卷并生成终局多维能力评估报告吗？系统将综合考评您的专业深度并生成职涯雷达图。'
    : '您尚未完成任何一轮考题作答，交卷将以初始摸底状态归档。确定要提前结束面试吗？'

  ElMessageBox.confirm(confirmMsg, '提前交卷并离会确认', {
    confirmButtonText: '交卷并查看终局报告',
    cancelButtonText: '继续留在面试间',
    type: 'warning'
  }).then(async () => {
    finishing.value = true
    try {
      if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel()
      }
      if (speechRecognitionInstance) {
        speechRecognitionInstance.stop()
      }
      const res = await finishInterview(sessionId)
      ElMessage.success('交卷成功！终局报告已生成')
      goToReport()
    } catch (err) {
      ElMessage.error(err.message || '交卷失败')
    } finally {
      finishing.value = false
    }
  }).catch(() => {})
}

// ==================== 生命周期钩子 ====================
watch(isInterviewerSpeaking, (speaking) => {
  if (speaking) {
    startTaffyLipSync()
  } else {
    stopTaffyLipSync()
  }
})

watch(answerMode, (newMode) => {
  if (newMode === 'code' && isRecording.value) {
    stopSpeechRecordingOnly()
  }
})


onMounted(async () => {
  const savedCfg = localStorage.getItem('interview_gemini_config')
  if (savedCfg) {
    try {
      Object.assign(geminiConfig, JSON.parse(savedCfg))
    } catch (e) {}
  }

  const savedTts = localStorage.getItem('interview_tts_config')
  if (savedTts) {
    try {
      Object.assign(ttsConfig, JSON.parse(savedTts))
    } catch (e) {}
  }

  await loadSession()
  initXiaoxiaoVoice()
  initCandidateMedia()

  timerInterval = setInterval(() => {
    if (!isCompleted.value) {
      timerSeconds.value++
    }
  }, 1000)

  nextTick(() => {
    if (currentTurn.value?.question && !isCompleted.value) {
      setTimeout(() => {
        speakQuestionText(currentTurn.value.question)
      }, 800)
    }
  })
})

onBeforeUnmount(() => {
  stopTaffyLipSync()
  if (timerInterval) clearInterval(timerInterval)
  if (virtualAudioInterval) clearInterval(virtualAudioInterval)
  if (animFrameId) cancelAnimationFrame(animFrameId)
  if (ttsHeartbeatInterval) clearInterval(ttsHeartbeatInterval)
  if (bargeInResetTimeout) clearTimeout(bargeInResetTimeout)

  if (candidateAudioCtx) {
    try {
      candidateAudioCtx.close()
    } catch (e) {}
  }

  if (candidateStream) {
    candidateStream.getTracks().forEach(track => track.stop())
  }

  stopAllTts()

  if (speechRecognitionInstance) {
    try {
      speechRecognitionInstance.stop()
    } catch (e) {}
    speechRecognitionInstance = null
  }
})
</script>
<style scoped>
/* ==================== 腾讯会议全局暗夜极简沉浸画布 ==================== */
.tc-meeting-root {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  z-index: 2500 !important;
  background-color: #111215;
  color: #e5e7eb;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  overflow: hidden;
  user-select: none;
  display: flex;
  flex-direction: column;
}

.tc-meeting-root.fullscreen-mode {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 9999;
}

/* ==================== 1. 腾讯会议顶部条 (Top Bar) ==================== */
.tc-top-bar {
  height: 52px;
  background: #18191c;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  flex-shrink: 0;
  z-index: 20;
}

.tc-top-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.tc-brand-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #60a5fa;
  background: rgba(37, 99, 235, 0.15);
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid rgba(59, 130, 246, 0.25);
}

.tc-meeting-id-pill {
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.06);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.tc-meeting-id-pill:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #f1f5f9;
}

.tc-meeting-id-pill .id-num {
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #e2e8f0;
}

.tc-net-quality {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 14px;
  padding: 0 4px;
}

.tc-net-quality .sig-bar {
  width: 3px;
  background: #00c06d;
  border-radius: 1px;
}

.tc-net-quality .bar-1 { height: 4px; }
.tc-net-quality .bar-2 { height: 7px; }
.tc-net-quality .bar-3 { height: 10px; }
.tc-net-quality .bar-4 { height: 13px; }

.tc-net-quality .sig-text {
  font-size: 11px;
  color: #00c06d;
  margin-left: 4px;
  line-height: 1;
}

.tc-rec-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(239, 68, 68, 0.12);
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #ef4444;
  font-weight: 600;
}

.rec-blink-dot {
  width: 7px;
  height: 7px;
  background: #ef4444;
  border-radius: 50%;
  animation: tc-rec-pulse 1.2s infinite;
}

@keyframes tc-rec-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(0.85); }
}

.tc-top-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.tc-session-title {
  font-size: 13px;
  font-weight: 600;
  color: #f8fafc;
}

.tc-stage-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #64748b;
}

.tc-stage-indicator .stage-node {
  display: flex;
  align-items: center;
  gap: 3px;
  transition: all 0.2s;
}

.tc-stage-indicator .stage-node.active {
  color: #38bdf8;
}

.tc-stage-indicator .stage-node.current {
  color: #00c06d;
  font-weight: 600;
}

.tc-stage-indicator .depth-tag {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  padding: 1px 6px;
  border-radius: 3px;
  margin-left: 6px;
}

.tc-top-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tc-turn-counter {
  font-size: 12px;
  color: #94a3b8;
  background: rgba(255, 255, 255, 0.05);
  padding: 4px 10px;
  border-radius: 6px;
}

.tc-turn-counter b {
  color: #38bdf8;
}

.tc-switch-btn {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #94a3b8 !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
}

.tc-switch-btn.el-button--primary {
  background: #2563eb !important;
  color: #ffffff !important;
  border-color: #2563eb !important;
}

.tc-tool-icon-btn {
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
  color: #cbd5e1 !important;
}

.tc-back-hall-btn {
  color: #ef4444 !important;
  font-size: 13px;
  margin-left: 4px;
}

.tc-back-hall-btn:hover {
  text-decoration: underline;
}

/* ==================== 2. 腾讯会议主视频舞台 ==================== */
.tc-main-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 10px 14px 76px 14px;
  position: relative;
  overflow: hidden;
  min-height: 0;
  gap: 10px;
}

.tc-video-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  min-height: 0;
  height: 100%;
}

.layout-speaker .tc-video-grid {
  grid-template-columns: 3fr 1fr;
}

/* 视窗卡片基底 */
.tc-tile {
  background: #181a1f;
  border-radius: 12px;
  border: 2px solid transparent;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35);
  transition: border-color 0.25s, box-shadow 0.25s;
}

/* 腾讯会议标志性亮绿发言边框 */
.tc-tile.active-speaker {
  border-color: #00c06d !important;
  box-shadow: 0 0 24px rgba(0, 192, 109, 0.45) !important;
}

.tc-tile.is-barge-interrupted {
  border-color: #fbbf24 !important;
  box-shadow: 0 0 24px rgba(245, 158, 11, 0.45) !important;
}

/* 视窗头部 */
.tile-header {
  position: absolute;
  top: 10px;
  left: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 10;
  pointer-events: auto;
}

.tile-status-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  color: #e2e8f0;
}

.tile-status-tag .status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #94a3b8;
}

.tile-status-tag .status-dot.live {
  background: #00c06d;
  box-shadow: 0 0 8px #00c06d;
}

.tile-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.persona-switch-link {
  font-size: 12px;
  color: #f472b6;
  background: rgba(244, 114, 182, 0.15);
  padding: 3px 8px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid rgba(244, 114, 182, 0.3);
  transition: all 0.2s;
}

.persona-switch-link:hover {
  background: rgba(244, 114, 182, 0.25);
  color: #fbcfe8;
}

.activity-pill {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  color: #cbd5e1;
}

.activity-pill.speaking {
  color: #00c06d;
  background: rgba(0, 192, 109, 0.15);
  border: 1px solid rgba(0, 192, 109, 0.3);
}

.activity-pill.interrupted {
  color: #fbbf24;
  background: rgba(245, 158, 11, 0.2);
  border: 1px solid rgba(245, 158, 11, 0.4);
}

/* ==================== 考官永雏塔菲 Live2D / 交互动画 ==================== */
.avatar-canvas-box {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: radial-gradient(circle at center, #262a34 0%, #16181d 100%);
  overflow: hidden;
}

.avatar-canvas-box::before {
  content: '';
  position: absolute;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(244, 114, 182, 0.16) 0%, rgba(56, 189, 248, 0.06) 50%, transparent 75%);
  pointer-events: none;
  filter: blur(24px);
  z-index: 1;
}

.tc-voice-halo {
  position: absolute;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s;
}

.tc-voice-halo.pulsing {
  opacity: 1;
}

.halo-layer {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid rgba(0, 192, 109, 0.4);
  animation: tc-halo-pulse 2s cubic-bezier(0.2, 0.8, 0.2, 1) infinite;
}

.halo-layer.layer-2 {
  animation-delay: 0.8s;
  border-color: rgba(56, 189, 248, 0.3);
}

@keyframes tc-halo-pulse {
  0% { transform: scale(0.7); opacity: 0.8; }
  100% { transform: scale(1.4); opacity: 0; }
}

.taffy-live-container {
  width: 100%;
  height: 100%;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  z-index: 2;
}

.taffy-model-wrapper {
  width: 100%;
  height: 100%;
  max-height: 520px;
  min-height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transform-origin: center bottom;
  animation: taffy-breath 3.6s ease-in-out infinite;
  transition: transform 0.25s ease;
}

.taffy-sprite-img {
  height: 88%;
  max-height: 480px;
  min-height: 260px;
  width: auto;
  max-width: 90%;
  object-fit: contain;
  filter: drop-shadow(0 12px 30px rgba(0, 0, 0, 0.55));
  user-select: none;
  pointer-events: none;
}

/* 说话时：头部语调微点头动效 */
.taffy-live-container.is-speaking .taffy-model-wrapper {
  animation: taffy-speaking-cadence 0.35s ease-in-out infinite alternate !important;
}

/* 倾听时：侧头倾听微动 */
.taffy-live-container.listening .taffy-model-wrapper {
  animation: taffy-attentive-nod 2.8s ease-in-out infinite !important;
}

/* 打断时：惊愕后撤 */
.taffy-live-container.is-interrupted .taffy-model-wrapper {
  animation: taffy-recoil 0.45s cubic-bezier(0.175, 0.885, 0.32, 1.275) !important;
}

@keyframes taffy-breath {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-5px) scale(1.008); }
}

@keyframes taffy-speaking-cadence {
  0% { transform: translateY(0) rotate(0deg); }
  100% { transform: translateY(-4px) rotate(-1deg); }
}

@keyframes taffy-attentive-nod {
  0%, 100% { transform: rotate(0deg) translateY(0); }
  35% { transform: rotate(2deg) translateY(-2px); }
  70% { transform: rotate(-1.5deg) translateY(-1px); }
}

@keyframes taffy-recoil {
  0% { transform: scale(1) translateY(0); }
  30% { transform: scale(0.96) translateY(-8px) rotate(-2deg); }
  100% { transform: scale(1) translateY(0) rotate(0deg); }
}

/* 备用风格考官 (阿里P7等)，保证永远不黑屏 */
.digital-human-avatar {
  width: 100%;
  height: 100%;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.digital-human-avatar .avatar-svg {
  width: 240px;
  height: 280px;
  max-height: 80%;
  object-fit: contain;
}

/* ==================== 腾讯会议经典左下角名片胶囊 ==================== */
.tc-participant-badge {
  position: absolute;
  bottom: 12px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(20, 22, 26, 0.75);
  backdrop-filter: blur(10px);
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 12px;
  color: #f1f5f9;
  z-index: 10;
}

.mic-status-icon {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 12px;
}

.mic-status-icon .wave-line {
  width: 2px;
  background: #94a3b8;
  border-radius: 1px;
}

.mic-status-icon.is-speaking .wave-line {
  background: #00c06d;
  animation: mic-wave 0.4s ease-in-out infinite alternate;
}

.mic-status-icon .line-1 { height: 4px; }
.mic-status-icon .line-2 { height: 8px; animation-delay: 0.15s; }
.mic-status-icon .line-3 { height: 12px; animation-delay: 0.3s; }

@keyframes mic-wave {
  0% { height: 3px; }
  100% { height: 12px; }
}

.host-tag {
  background: #2563eb;
  color: #ffffff;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 3px;
  margin-left: 2px;
}

.me-tag {
  background: #059669;
  color: #ffffff;
  font-size: 10px;
  padding: 1px 4px;
  border-radius: 3px;
  margin-left: 2px;
}

.tile-corner-tools {
  position: absolute;
  bottom: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 10;
}

.tc-mini-btn {
  background: rgba(0, 0, 0, 0.6) !important;
  border-color: rgba(255, 255, 255, 0.15) !important;
  color: #ffffff !important;
}

/* ==================== 候选人视窗与声浪 ==================== */
.candidate-video-viewport {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #0f1012;
  overflow: hidden;
}

.candidate-video-elem {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.candidate-video-elem.stream-hidden {
  display: none;
}

.tc-camera-off-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #262930;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38px;
  border: 2px solid rgba(255, 255, 255, 0.1);
}

.avatar-hint {
  font-size: 12px;
  color: #64748b;
}

.candidate-vu-hud {
  position: absolute;
  top: 50px;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(8px);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
}

.candidate-vu-hud .vu-tag.barge-highlight {
  color: #fbbf24;
  font-weight: 700;
}

.vu-bars {
  display: flex;
  align-items: flex-end;
  gap: 2px;
  height: 18px;
}

.vu-bar {
  width: 3px;
  background: #00c06d;
  border-radius: 1px;
  min-height: 4px;
}

.vu-bar.barge-bar {
  background: #fbbf24;
}

.vu-wpm {
  color: #38bdf8;
  font-weight: 600;
  margin-left: 4px;
}

/* ==================== 屏幕共享与手撕代码模式 ==================== */
.tc-screen-share-stage {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #18191c;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.share-top-banner {
  height: 42px;
  background: #1e2024;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  font-size: 13px;
  color: #f1f5f9;
}

.share-top-banner .share-icon {
  margin-right: 6px;
}

.stop-share-btn {
  background: rgba(239, 68, 68, 0.2) !important;
  color: #f87171 !important;
  border-color: rgba(239, 68, 68, 0.35) !important;
}

.share-workspace-grid {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 280px;
  height: calc(100% - 42px);
  overflow: hidden;
}

.share-code-pane {
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  padding: 12px;
  overflow-y: auto;
}

.code-pane-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.code-textarea-dark :deep(.el-textarea__inner) {
  background: #121316 !important;
  color: #e2e8f0 !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  font-family: "Fira Code", Monaco, Consolas, Courier, monospace !important;
  font-size: 13px !important;
  line-height: 1.6 !important;
}

.socratic-hint-banner {
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 10px;
  font-size: 12px;
  color: #fef3c7;
}

.socratic-hint-banner .hint-header {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
  margin-bottom: 4px;
}

.live-sandbox-panel {
  background: #121316;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 10px;
  margin-top: 10px;
  font-size: 12px;
}

.live-sandbox-panel .sandbox-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  color: #94a3b8;
}

.sandbox-output {
  margin: 0;
  font-family: monospace;
  color: #a7f3d0;
  max-height: 120px;
  overflow-y: auto;
}

/* 画中画 (PIP) 考官视窗 */
.share-pip-pane {
  background: #141518;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.pip-interviewer-card {
  background: #1d1f24;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.pip-header {
  font-size: 11px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 8px;
}

.pip-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #00c06d;
}

.pip-avatar-view {
  width: 140px;
  height: 140px;
}

.pip-question-bubble {
  font-size: 12px;
  color: #e2e8f0;
  background: rgba(255, 255, 255, 0.05);
  padding: 8px;
  border-radius: 6px;
  margin-top: 8px;
  line-height: 1.5;
}

/* ==================== 3. 腾讯会议中央考题看板与实时对谈流 ==================== */
.tc-question-board {
  flex-shrink: 0;
  max-height: 160px;
  background: rgba(24, 26, 32, 0.92);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 10px 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow-y: auto;
  z-index: 15;
  transition: border-color 0.25s, box-shadow 0.25s;
}

.tc-question-board.barge-alert {
  border-color: #fbbf24;
  box-shadow: 0 0 20px rgba(245, 158, 11, 0.35);
}

.q-board-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  padding-bottom: 6px;
}

.q-meta-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.q-stage-tag {
  background: #2563eb;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}

.q-dim-tag {
  color: #94a3b8;
  font-size: 12px;
}

.q-meta-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.q-tts-tag {
  color: #f472b6;
  background: rgba(244, 114, 182, 0.15);
  border: 1px solid rgba(244, 114, 182, 0.25);
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.q-mini-btn {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(255, 255, 255, 0.15) !important;
  color: #f1f5f9 !important;
}

.q-barge-btn {
  font-weight: 700;
  border-radius: 6px;
}

.q-content-box {
  display: flex;
  align-items: baseline;
  gap: 6px;
  line-height: 1.5;
}

.q-speaker-label {
  color: #f472b6;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}

.q-text {
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.q-answer-stream {
  display: flex;
  align-items: baseline;
  gap: 6px;
  background: rgba(56, 189, 248, 0.08);
  border-left: 3px solid #38bdf8;
  padding: 4px 8px;
  border-radius: 0 4px 4px 0;
  font-size: 13px;
  line-height: 1.4;
}

.a-speaker-label {
  color: #38bdf8;
  font-weight: 700;
  flex-shrink: 0;
}

.a-text {
  color: #e0f2fe;
}

.typing-cursor {
  color: #38bdf8;
  animation: blink-cursor 0.8s infinite;
}

@keyframes blink-cursor {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* ==================== 4. 腾讯会议经典底部控制栏 (Bottom Dock) ==================== */
.tc-bottom-dock {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(28, 30, 36, 0.94);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
  padding: 6px 14px;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.55);
  z-index: 40;
}

.dock-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.dock-btn-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 8px;
  transition: all 0.2s;
  min-width: 50px;
}

.dock-btn-group:hover {
  background: rgba(255, 255, 255, 0.08);
}

.dock-icon-circle {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  position: relative;
  transition: all 0.2s;
}

.dock-btn-group:hover .dock-icon-circle {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.15);
}

.dock-btn-label {
  font-size: 11px;
  color: #94a3b8;
  white-space: nowrap;
}

.dock-separator {
  width: 1px;
  height: 28px;
  background: rgba(255, 255, 255, 0.1);
  margin: 0 4px;
}

/* 核心插话打断按钮 */
.dock-btn-group.barge-btn .dock-icon-circle {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.4);
}

.dock-btn-group.barge-btn.barge-available .dock-icon-circle {
  animation: tc-barge-pulse 1s infinite alternate;
}

@keyframes tc-barge-pulse {
  0% { box-shadow: 0 0 4px rgba(245, 158, 11, 0.4); }
  100% { box-shadow: 0 0 16px rgba(245, 158, 11, 0.9); }
}

/* 主录音/作答按钮 */
.dock-btn-group.record-main-btn.recording .dock-icon-circle {
  background: #ef4444;
  animation: tc-rec-ring 1.2s infinite;
}

@keyframes tc-rec-ring {
  0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7); }
  70% { box-shadow: 0 0 0 10px rgba(239, 68, 68, 0); }
  100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
}

/* 红色结束面试按钮 */
.dock-btn-group.leave-btn .dock-icon-circle {
  background: #dc2626;
}

.dock-btn-group.leave-btn:hover .dock-icon-circle {
  background: #ef4444;
}

.dock-btn-group.leave-btn .dock-btn-label {
  color: #fca5a5;
  font-weight: 600;
}

.dock-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  background: #3b82f6;
  color: #ffffff;
  font-size: 10px;
  font-weight: 700;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.mic-live-vu {
  position: absolute;
  bottom: 4px;
  right: 6px;
  width: 4px;
  height: 14px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 2px;
  display: flex;
  align-items: flex-end;
}

.vu-mini-bar {
  width: 100%;
  background: #00c06d;
  border-radius: 2px;
}

/* ==================== 抽屉与弹窗深色定制 ==================== */
.history-drawer-custom :deep(.el-drawer) {
  background: #18191c !important;
  color: #e2e8f0 !important;
}

.drawer-turn-card {
  background: #202228;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 12px;
}

.turn-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.drawer-qa-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.qa-item {
  display: flex;
  gap: 10px;
  font-size: 13px;
  line-height: 1.6;
}

.qa-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  white-space: nowrap;
  height: fit-content;
}

.qa-item.q .qa-tag {
  background: rgba(244, 114, 182, 0.2);
  color: #f472b6;
}

.qa-item.a .qa-tag {
  background: rgba(56, 189, 248, 0.2);
  color: #38bdf8;
}

.drawer-feedback-box {
  background: rgba(0, 0, 0, 0.25);
  border-radius: 8px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 12px;
}

.fb-title {
  font-weight: 700;
  color: #fbbf24;
  margin-bottom: 3px;
}

.fb-item.standard .fb-title {
  color: #34d399;
}

.tc-dropdown-dark {
  background: #202228 !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
}

.tc-dropdown-dark :deep(.el-dropdown-menu__item) {
  color: #cbd5e1 !important;
}

.tc-dropdown-dark :deep(.el-dropdown-menu__item:hover) {
  background: #2d313a !important;
  color: #f8fafc !important;
}

.gemini-test-msg {
  font-size: 12px;
  margin-left: 10px;
}

.gemini-test-msg.success {
  color: #00c06d;
}

.gemini-test-msg.error {
  color: #ef4444;
}

.gemini-test-msg.warning {
  color: #f59e0b;
}

.tc-engine-tabs :deep(.el-tabs__item) {
  color: #94a3b8;
  font-size: 14px;
}

.tc-engine-tabs :deep(.el-tabs__item.is-active) {
  color: #38bdf8;
  font-weight: 600;
}

.tc-engine-tabs :deep(.el-tabs__active-bar) {
  background-color: #38bdf8;
}

.tc-engine-tabs :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(255, 255, 255, 0.08);
}

.tts-mode-selector {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.form-item-tip {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  line-height: 1.4;
}

.score-high { color: #00c06d; font-weight: 700; }
.score-mid { color: #38bdf8; font-weight: 700; }
.score-low { color: #f59e0b; font-weight: 700; }
</style>
