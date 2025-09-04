# ☁️ SonarCloud Setup Guide

> **FREE for public repositories - 5 minute setup**

## 🚀 Quick Setup

### Step 1: Create SonarCloud Account
1. Go to [SonarCloud.io](https://sonarcloud.io)
2. Click "Log in" → "With GitHub"
3. Authorize SonarCloud to access your GitHub account

### Step 2: Import Your Repository
1. Click "+" → "Analyze new project"
2. Select your GitHub organization: `thiago2santos`
3. Choose `wallet-service` repository
4. Click "Set up"

### Step 3: Get Your Token
1. Go to "My Account" → "Security"
2. Generate new token: `wallet-service-ci`
3. Copy the token (you'll need it for GitHub)

### Step 4: Configure GitHub Secrets
1. Go to your GitHub repository
2. Settings → Secrets and variables → Actions
3. Click "New repository secret"
4. Name: `SONAR_TOKEN`
5. Value: [paste your token]
6. Click "Add secret"

### Step 5: Update Project Configuration
Your project can be configured to use:
- **Project Key:** `thiago2santos_wallet-service`
- **Organization:** `thiago2santos`

## 📊 What You Get (FREE)

- ✅ **Code Quality Metrics** - Maintainability rating
- ✅ **Security Analysis** - Vulnerability detection
- ✅ **Code Coverage** - Integration with JaCoCo
- ✅ **Code Smells** - Best practice violations
- ✅ **Duplication Analysis** - Copy-paste detection
- ✅ **PR Decoration** - Comments on pull requests
- ✅ **Quality Gate** - Pass/fail criteria
- ✅ **Historical Trends** - Track improvements over time

## 🎯 Free Tier Limits
- ✅ **Unlimited public repositories**
- ✅ **Unlimited analysis**
- ✅ **All features included**
- ⚠️ **1 private repository only**

## 🔧 Troubleshooting

### Common Issues
1. **"Project not found"** - Check project key matches exactly
2. **"Token invalid"** - Regenerate token in SonarCloud
3. **"No coverage"** - Ensure JaCoCo runs before SonarCloud

### Verify Setup
```bash
# Test locally (optional)
./mvnw clean verify sonar:sonar \
  -Dsonar.projectKey=thiago2santos_wallet-service \
  -Dsonar.organization=thiago2santos \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.token=YOUR_TOKEN
```

## 📈 Expected Results
After setup, you'll see:
- Quality gate status in PRs
- Detailed analysis at: https://sonarcloud.io/project/overview?id=thiago2santos_wallet-service
- Code coverage trends
- Security hotspot alerts

**Total Setup Time:** ~5 minutes
**Ongoing Cost:** $0 (FREE forever for public repos)
