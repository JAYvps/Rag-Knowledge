<!-- ============ src/views/HomeView.vue ============ -->
<!--
  Clean dashboard:
  - Left-aligned hero (no centered text, DESIGN_VARIANCE=8)
  - Bento-style stats grid
  - No particles, no purple gradients
  - Emerald accent, muted neutrals
  - Staggered entrance animations
-->
<template>
  <div class="home" ref="containerRef">
    <!-- Hero — asymmetric layout -->
    <div class="hero-section" ref="heroRef">
      <div class="hero-content">
        <h1 class="hero-title" ref="heroTitle">欢迎使用</h1>
        <p class="hero-subtitle" ref="heroSubtitle">基于 RAG 向量检索技术，让知识触手可及</p>
      </div>
    </div>

    <!-- Stats — bento grid -->
    <div class="stats-grid" ref="statsGrid">
      <div class="stat-card stat-card--primary" ref="statCard1">
        <div class="stat-icon">
          <el-icon :size="20"><Collection /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.repoCount }}</span>
          <span class="stat-label">知识库</span>
        </div>
      </div>

      <div class="stat-card" ref="statCard2">
        <div class="stat-icon stat-icon--green">
          <el-icon :size="20"><Document /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ stats.docCount }}</span>
          <span class="stat-label">文档总数</span>
        </div>
      </div>

      <div class="stat-card stat-card--wide" ref="statCard3">
        <div class="stat-icon stat-icon--amber">
          <el-icon :size="20"><ChatDotRound /></el-icon>
        </div>
        <div class="stat-content">
          <span class="stat-value">已上线</span>
          <span class="stat-label">智能问答</span>
        </div>
      </div>
    </div>

    <!-- Quick Start -->
    <div class="quickstart-section" ref="quickStartRef">
      <div class="section-header">
        <h2 class="section-title">快速开始</h2>
        <span class="section-badge">3 步搞定</span>
      </div>
      <div class="steps-grid">
        <div class="step-card" v-for="(step, i) in steps" :key="i">
          <div class="step-number">{{ String(i + 1).padStart(2, '0') }}</div>
          <div class="step-body">
            <h3 class="step-title">{{ step.title }}</h3>
            <p class="step-desc">{{ step.desc }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { getRepoList } from '../api/kb'
import gsap from 'gsap'

const stats = ref({ repoCount: 0, docCount: 0 })
const containerRef = ref(null)
const heroRef = ref(null)
const heroTitle = ref(null)
const heroSubtitle = ref(null)
const statsGrid = ref(null)
const statCard1 = ref(null)
const statCard2 = ref(null)
const statCard3 = ref(null)
const quickStartRef = ref(null)
let ctx
let countTween = null

const steps = [
  { title: '浏览知识库', desc: '查看已同步的企业知识库' },
  { title: '上传文档', desc: '上传你的个人文档' },
  { title: '智能问答', desc: '基于全部知识库进行 AI 问答' }
]

function animateStats(repoCount, docCount) {
  const proxy = { repo: 0, doc: 0 }
  countTween = gsap.to(proxy, {
    repo: repoCount,
    doc: docCount,
    duration: 1.5,
    ease: 'power2.out',
    onUpdate() {
      stats.value.repoCount = Math.round(proxy.repo)
      stats.value.docCount = Math.round(proxy.doc)
    }
  })
}

onMounted(async () => {
  if (!containerRef.value) return

  ctx = gsap.context(() => {
    const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })

    // Hero stagger
    tl.from(heroTitle.value, { y: 24, opacity: 0, duration: 0.6 })
    tl.from(heroSubtitle.value, { y: 16, opacity: 0, duration: 0.5 }, '-=0.3')

    // Stats cards stagger
    const cards = [statCard1.value, statCard2.value, statCard3.value]
    cards.forEach((card, i) => {
      if (!card) return
      gsap.from(card, {
        y: 24,
        opacity: 0,
        duration: 0.5,
        ease: 'power3.out',
        delay: 0.4 + i * 0.1
      })
    })

    // Quick start section
    gsap.from(quickStartRef.value, {
      y: 24,
      opacity: 0,
      duration: 0.6,
      ease: 'power3.out',
      delay: 0.7
    })

    gsap.from('.step-card', {
      y: 16,
      opacity: 0,
      stagger: 0.08,
      duration: 0.4,
      ease: 'power3.out',
      delay: 0.9
    })
  }, containerRef.value)

  // Fetch data
  try {
    const res = await getRepoList()
    const repos = res.data || []
    animateStats(repos.length, repos.reduce((sum, r) => sum + (r.docCount || 0), 0))
  } catch (e) {
    // ignore
  }
})

onUnmounted(() => {
  countTween?.kill()
  ctx?.revert()
})
</script>

<style scoped>
.home {
  position: relative;
  min-height: 100%;
  padding-bottom: 40px;
}

/* ===== Hero — asymmetric ===== */
.hero-section {
  padding: 20px 0 32px;
}

.hero-content {
  max-width: 600px;
}

.hero-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.5px;
  margin-bottom: 8px;
}

.hero-subtitle {
  font-size: 15px;
  color: var(--text-muted);
  line-height: 1.6;
}

/* ===== Stats — bento grid ===== */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 28px;
}

.stat-card {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: border-color var(--duration) var(--ease), box-shadow var(--duration) var(--ease);
}

.stat-card:hover {
  border-color: var(--accent-border);
  box-shadow: var(--shadow-md);
}

.stat-card--wide {
  grid-column: span 2;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--accent-muted);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon--green {
  background: rgba(34, 197, 94, 0.08);
  color: #16a34a;
}

.stat-icon--amber {
  background: rgba(245, 158, 11, 0.08);
  color: #d97706;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.5px;
  line-height: 1.1;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  font-weight: 500;
}

/* ===== Quick Start ===== */
.quickstart-section {
  background: var(--bg-elevated);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 28px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.section-badge {
  font-size: 11px;
  padding: 2px 10px;
  background: var(--accent-muted);
  color: var(--accent);
  border-radius: 20px;
  font-weight: 600;
  border: 1px solid var(--accent-border);
}

.steps-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.step-card {
  display: flex;
  gap: 14px;
  padding: 16px;
  border-radius: var(--radius-md);
  background: var(--bg-muted);
  border: 1px solid var(--border-subtle);
  transition: border-color var(--duration) var(--ease);
}

.step-card:hover {
  border-color: var(--border);
}

.step-number {
  font-size: 24px;
  font-weight: 800;
  color: var(--border);
  font-variant-numeric: tabular-nums;
  line-height: 1;
  flex-shrink: 0;
}

.step-body {
  flex: 1;
}

.step-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.step-desc {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.5;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  .stat-card--wide {
    grid-column: span 1;
  }
  .steps-grid {
    grid-template-columns: 1fr;
  }
}
</style>
