<!-- ============ src/views/auth/LoginView.vue ============ -->
<!--
  Clean login page:
  - Split-screen layout (asymmetric, text left / form right)
  - No orbs, no particles, no purple gradients
  - Emerald accent, off-black text, subtle glass card
  - Spring-physics entrance animations
-->
<template>
  <div class="login-page" ref="containerRef">
    <!-- Left: Brand panel -->
    <div class="brand-panel" ref="brandPanel">
      <div class="brand-content">
        <div class="brand-mark" ref="brandMark">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <rect width="40" height="40" rx="10" fill="var(--accent)"/>
            <path d="M12 14h16M12 20h11M12 26h14" stroke="#fff" stroke-width="2.2" stroke-linecap="round"/>
          </svg>
        </div>
        <h2 class="brand-title" ref="brandTitle">RAG 智能知识库</h2>
        <p class="brand-desc" ref="brandDesc">
          基于检索增强生成技术<br/>
          让企业知识真正流动起来
        </p>
        <div class="brand-features" ref="brandFeatures">
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>向量检索，精准定位</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>多源知识，统一管理</span>
          </div>
          <div class="feature-item">
            <div class="feature-dot"></div>
            <span>流式问答，实时反馈</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Login form -->
    <div class="form-panel">
      <div class="form-card" ref="cardRef">
        <div class="form-header" ref="formHeader">
          <h2 class="form-title">登录</h2>
          <p class="form-subtitle">输入你的账号信息</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="login-form">
          <div class="form-block" ref="formItem1">
            <label class="form-label">用户名</label>
            <el-form-item prop="username">
              <el-input
                  v-model="form.username"
                  placeholder="请输入用户名"
                  size="large"
              />
            </el-form-item>
          </div>

          <div class="form-block" ref="formItem2">
            <label class="form-label">密码</label>
            <el-form-item prop="password">
              <el-input
                  v-model="form.password"
                  placeholder="请输入密码"
                  type="password"
                  show-password
                  size="large"
                  @keyup.enter="handleLogin"
              />
            </el-form-item>
          </div>

          <div class="form-block" ref="formItem3">
            <el-form-item>
              <el-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  class="submit-btn"
                  @click="handleLogin"
              >
                登录
              </el-button>
            </el-form-item>
          </div>
        </el-form>

        <div class="form-footer" ref="footerRef">
          还没有账号？
          <router-link to="/register" class="footer-link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { login } from '../../api/auth'
import { ElMessage } from 'element-plus'
import gsap from 'gsap'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const containerRef = ref(null)
const cardRef = ref(null)
const brandPanel = ref(null)
const brandMark = ref(null)
const brandTitle = ref(null)
const brandDesc = ref(null)
const brandFeatures = ref(null)
const formHeader = ref(null)
const formItem1 = ref(null)
const formItem2 = ref(null)
const formItem3 = ref(null)
const footerRef = ref(null)
let ctx

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(form)
    userStore.setLoginInfo(res.data)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (!containerRef.value) return

  ctx = gsap.context(() => {
    const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })

    // Brand panel stagger
    tl.from(brandMark.value, { y: 20, opacity: 0, duration: 0.6 })
    tl.from(brandTitle.value, { y: 20, opacity: 0, duration: 0.5 }, '-=0.3')
    tl.from(brandDesc.value, { y: 16, opacity: 0, duration: 0.5 }, '-=0.3')
    tl.from('.feature-item', { x: -16, opacity: 0, stagger: 0.1, duration: 0.4 }, '-=0.2')

    // Form card
    tl.from(cardRef.value, { y: 32, opacity: 0, duration: 0.7 }, '-=0.6')
    tl.from(formHeader.value, { y: 12, opacity: 0, duration: 0.4 }, '-=0.4')
    tl.from(formItem1.value, { y: 12, opacity: 0, duration: 0.4 }, '-=0.3')
    tl.from(formItem2.value, { y: 12, opacity: 0, duration: 0.4 }, '-=0.3')
    tl.from(formItem3.value, { y: 12, opacity: 0, duration: 0.4 }, '-=0.3')
    tl.from(footerRef.value, { opacity: 0, duration: 0.4 }, '-=0.2')
  }, containerRef.value)
})

onUnmounted(() => { ctx?.revert() })
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  overflow: hidden;
}

/* ===== Brand Panel ===== */
.brand-panel {
  flex: 1;
  background: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  position: relative;
}

.brand-content {
  max-width: 400px;
}

.brand-mark {
  margin-bottom: 32px;
}

.brand-title {
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -0.5px;
  line-height: 1.2;
  margin-bottom: 16px;
}

.brand-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1.8;
  margin-bottom: 40px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.65);
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  flex-shrink: 0;
}

/* ===== Form Panel ===== */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: var(--bg);
}

.form-card {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 32px;
}

.form-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.3px;
  margin-bottom: 8px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--text-muted);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-block {
  margin-bottom: 4px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--radius-sm) !important;
  margin-top: 8px;
  transition: transform 0.1s var(--ease);
}
.submit-btn:active {
  transform: scale(0.98);
}

.form-footer {
  text-align: center;
  color: var(--text-muted);
  font-size: 13px;
  margin-top: 24px;
}

.footer-link {
  color: var(--accent);
  text-decoration: none;
  font-weight: 600;
}
.footer-link:hover {
  color: var(--accent-hover);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }
  .brand-panel {
    padding: 32px 24px;
    min-height: auto;
  }
  .brand-title {
    font-size: 24px;
  }
  .form-panel {
    padding: 32px 24px;
  }
}
</style>
