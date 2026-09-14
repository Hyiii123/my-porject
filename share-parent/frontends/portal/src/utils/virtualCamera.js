// 虚拟全息摄像头视频流生成器 (针对 HTTP IP 访问或无物理摄像头环境)
export const createVirtualCameraStream = (candidateName = '候选人') => {
  const canvas = document.createElement('canvas')
  canvas.width = 640
  canvas.height = 480
  const ctx = canvas.getContext('2d')

  let frame = 0
  let animId = null

  const draw = () => {
    frame++
    // 暗黑深空背景
    const grad = ctx.createRadialGradient(320, 240, 50, 320, 240, 360)
    grad.addColorStop(0, '#1e293b')
    grad.addColorStop(1, '#020617')
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, 640, 480)

    // 科技网格
    ctx.strokeStyle = 'rgba(56, 189, 248, 0.08)'
    ctx.lineWidth = 1
    for (let x = 0; x < 640; x += 40) {
      ctx.beginPath()
      ctx.moveTo(x, 0)
      ctx.lineTo(x, 480)
      ctx.stroke()
    }
    for (let y = 0; y < 480; y += 40) {
      ctx.beginPath()
      ctx.moveTo(0, y)
      ctx.lineTo(640, y)
      ctx.stroke()
    }

    // 扫描线
    const scanY = (frame * 2.5) % 480
    ctx.strokeStyle = 'rgba(14, 165, 233, 0.35)'
    ctx.lineWidth = 2
    ctx.beginPath()
    ctx.moveTo(0, scanY)
    ctx.lineTo(640, scanY)
    ctx.stroke()

    // 呼吸偏移
    const breath = Math.sin(frame * 0.04) * 5

    // 学员数字人剪影
    // 头部
    ctx.beginPath()
    ctx.arc(320, 190 + breath, 54, 0, Math.PI * 2)
    ctx.fillStyle = '#0ea5e9'
    ctx.fill()
    ctx.strokeStyle = '#38bdf8'
    ctx.lineWidth = 3
    ctx.stroke()

    // 眼睛
    const eyeBlink = Math.sin(frame * 0.03) > 0.96
    if (!eyeBlink) {
      ctx.fillStyle = '#ffffff'
      ctx.beginPath()
      ctx.arc(302, 185 + breath, 4, 0, Math.PI * 2)
      ctx.arc(338, 185 + breath, 4, 0, Math.PI * 2)
      ctx.fill()
    }

    // 耳麦
    ctx.strokeStyle = '#38bdf8'
    ctx.lineWidth = 3
    ctx.beginPath()
    ctx.arc(264, 190 + breath, 8, 0, Math.PI * 2)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(264, 198 + breath)
    ctx.lineTo(290, 215 + breath)
    ctx.stroke()

    // 肩膀身体
    ctx.beginPath()
    ctx.ellipse(320, 355 + breath, 130, 85, 0, Math.PI, 0)
    ctx.fillStyle = '#0f172a'
    ctx.fill()
    ctx.strokeStyle = '#0284c7'
    ctx.lineWidth = 2
    ctx.stroke()

    // 科技角标 HUD
    ctx.fillStyle = '#10b981'
    ctx.font = 'bold 15px monospace'
    ctx.fillText('● LIVE VIRTUAL CAMERA 720P', 25, 38)

    ctx.fillStyle = '#cbd5e1'
    ctx.font = '13px sans-serif'
    ctx.fillText(candidateName + ' · 全真视频信道正常接入', 25, 62)

    animId = requestAnimationFrame(draw)
  }
  draw()

  const stream = canvas.captureStream(30)
  stream._stopVirtualAnimation = () => {
    if (animId) cancelAnimationFrame(animId)
  }
  return stream
}
