<!-- ============ src/views/auth/RegisterView.vue ============ -->
<!--
  Clean register page — matches login aesthetic
-->
<template>
  <div class="register-page" ref="containerRef">
    <!-- Left: Brand panel -->
    <div class="brand-panel" ref="brandPanel">
      <div class="brand-content">
        <div class="brand-mark" ref="brandMark">
          <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
            <rect width="40" height="40" rx="10" fill="var(--accent)"/>
            <path d="M20 12v6M17 18h6M12 26h16M12 30h16" stroke="#fff" stroke-width="2.2" stroke-linecap="round"/>
          </svg>
        </div>
        <h2 class="brand-title" ref="brandTitle">创建账号</h2>
        <p class="brand-desc" ref="brandDesc">
          注册后即可使用全部功能<br/>
          上传文档、浏览知识库、智能问答
        </p>
      </div>
    </div>

    <!-- Right: Register form -->
    <div class="form-panel">
      <div class="form-card" ref="cardRef">
        <div class="form-header" ref="formHeader">
          <h2 class="form-title">注册</h2>
          <p class="form-subtitle">填写以下信息创建账号</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="register-form">
          <div class="form-block" ref="formItem1">
            <label class="form-label">用户名</label>
            <el-form-item prop="username">
              <el-input
                  v-model="form.username"
                  placeholder="3-20个字符"
                  size="large"
              />
            </el-form-item>
          </div>

          <div class="form-block" ref="formItem2">
            <label class="form-label">密码</label>
            <el-form-item prop="password">
              <el-input
                  v-model="form.password"
                  placeholder="至少6位"
                  type="password"
                  show-password
                  size="large"
              />
            </el-form-item>
          </div>

          <div class="form-block" ref="formItem3">
            <label class="form-label">确认密码</label>
            <el-form-item prop="confirmPassword">
              <el-input
                  v-model="form.confirmPassword"
                  placeholder="再次输入密码"
                  type="password"
                  show-password
                  size="large"
              />
            </el-form-item>
          </div>

          <div class="form-block" ref="formItem4">
            <el-form-item>
              <el-button
                  type="primary"
                  size="large"
                  :loading="loading"
                  class="submit-btn"
                  @click="handleRegister"
              >
                注册
              </el-button>
            </el-form-item>
          </div>
        </el-form>

        <div class="form-footer" ref="footerRef">
          已有账号？
          <router-link to="/login" class="footer-link">去登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../../api/auth'
import { ElMessage } from 'element-plus'
import gsap from 'gsap'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const containerRef = ref(null)
const cardRef = ref(null)
const brandPanel = ref(null)
const brandMark = ref(null)
const brandTitle = ref(null)
const brandDesc = ref(null)
const formHeader = ref(null)
const formItem1 = ref(null)
const formItem2 = ref(null)
const formItem3 = ref(null)
const formItem4 = ref(null)
const footerRef = ref(null)
let ctx

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({ username: form.username, password: form.password })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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

    tl.from(brandMark.value, { y: 20, opacity: 0, duration: 0.6 })
    tl.from(brandTitle.value, { y: 20, opacity: 0, duration: 0.5 }, '-=0.3')
    tl.from(brandDesc.value, { y: 16, opacity: 0, duration: 0.5 }, '-=0.3')

    tl.from(cardRef.value, { y: 32, opacity: 0, duration: 0.7 }, '-=0.4')
    tl.from(formHeader.value, { y: 12, opacity: 0, duration: 0.4 }, '-=0.4')
    tl.from(formItem1.value, { y: 12, opacity: 0, duration: 0.4 }, '-=0.3')
    tl.from(formItem2.value, { y: 12, opacity: 0, duration: 0.4 }, '-=0.3')
    tl.from(formItem3.value, { y: 12, opacity: 0, duration: 0.4 }, '-=0.3')
    tl.from(formItem4.value, { y: 12, opacity: 0, duration: 0.4 }, '-=0.3')
    tl.from(footerRef.value, { opacity: 0, duration: 0.4 }, '-=0.2')
  }, containerRef.value)
})

onUnmounted(() => { ctx?.revert() })
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  overflow: hidden;
}

.brand-panel {
  flex: 1;
  background: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.brand-content { max-width: 400px; }
.brand-mark { margin-bottom: 32px; }

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
}

.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: var(--bg);
}

.form-card { width: 100%; max-width: 380px; }
.form-header { margin-bottom: 32px; }

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

.register-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-block { margin-bottom: 4px; }

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
.submit-btn:active { transform: scale(0.98); }

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
.footer-link:hover { color: var(--accent-hover); }

@media (max-width: 768px) {
  .register-page { flex-direction: column; }
  .brand-panel { padding: 32px 24px; min-height: auto; }
  .brand-title { font-size: 24px; }
  .form-panel { padding: 32px 24px; }
}
</style>
