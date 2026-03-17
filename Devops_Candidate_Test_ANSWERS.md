# TECHNICAL ASSESSMENT — ANSWERS
**Senior DevOps / Cloud Engineer (AI-Augmented)**
Bangalore · 4+ Years Experience · AWS · GCP · Terraform · AI Tooling

---

## SECTION 1: Networking Fundamentals

### Q1. ❌ [5 marks] CIDR Subnetting

**VPC CIDR: 10.0.0.0/16**

**(a) 4 equal subnets — split /16 into /18:**

| Subnet | CIDR Block     |
|--------|----------------|
| 1      | 10.0.0.0/18    |
| 2      | 10.0.64.0/18   |
| 3      | 10.0.128.0/18  |
| 4      | 10.0.192.0/18  |

**(b) Usable IPs per subnet:**
- /18 = 2^(32-18) = 16,384 total IPs
- AWS reserves 5 per subnet (network address, VPC router, DNS, future use, broadcast)
- **Usable = 16,384 − 5 = 16,379 usable IP addresses**

**(c) Public vs Private assignment:**
- **Public:** 10.0.0.0/18 — hosts internet-facing resources (ALB, bastion, NAT GW). Needs a route to IGW and `map_public_ip_on_launch = true`.
- **Private:** 10.0.64.0/18, 10.0.128.0/18, 10.0.192.0/18 — host app servers, databases, internal services. No direct route to IGW; outbound only via NAT Gateway.
  Justification: minimises attack surface by keeping application/data tiers unreachable from the internet.

---

### Q2. ❌ [5 marks] NAT Gateway Setup

**Goal:** Private EC2 downloads from internet; NOT directly reachable from internet.

**Required AWS Components:**
1. **VPC** — contains both subnets.
2. **Internet Gateway (IGW)** — attached to the VPC.
3. **Public Subnet** — route table has `0.0.0.0/0 → IGW`.
4. **Elastic IP (EIP)** — allocated and associated with the NAT Gateway.
5. **NAT Gateway** — placed **in the public subnet**, uses the EIP.
6. **Private Subnet** — where the EC2 instance lives.
7. **Private Route Table** — `0.0.0.0/0 → NAT Gateway`.

**Route Table Entries:**

| Route Table         | Destination | Target        |
|---------------------|-------------|---------------|
| Public Route Table  | 10.0.0.0/16 | local         |
| Public Route Table  | 0.0.0.0/0   | igw-xxxxxxxx  |
| Private Route Table | 10.0.0.0/16 | local         |
| Private Route Table | 0.0.0.0/0   | nat-xxxxxxxx  |

**Traffic flow:**
EC2 (private) → Private RT → NAT GW → IGW → Internet
Inbound from internet is blocked — NAT GW performs source NAT and does not accept unsolicited inbound connections.

---

### Q3. ❌ [4 marks] IGW vs NAT Gateway

| Property                           | Internet Gateway (IGW)              | NAT Gateway                              |
|------------------------------------|-------------------------------------|------------------------------------------|
| Direction of traffic               | Bidirectional (inbound + outbound)  | Outbound only (egress)                   |
| Requires public IP on instance?    | Yes — instance needs public/EIP     | No — NAT GW holds the EIP, not the instance |
| Allows inbound from internet?      | Yes                                 | No — rejects unsolicited inbound         |
| Where does it live?                | VPC-level (attached to VPC)         | Public subnet (resides in a subnet)      |

---

### Q4. [3 marks] Cross-Region VPC Connectivity

**Approach 1: Inter-Region VPC Peering**
- Direct, private routing between two VPCs across regions.
- **Trade-off:** No transitive routing — VPC-A→B and B→C does not allow A→C. Becomes hard to manage at scale.

**Approach 2: AWS Transit Gateway + Inter-Region Peering**
- Create a TGW in each region, peer the TGWs, attach VPCs to their regional TGW.
- **Trade-off:** Additional cost per attachment and per GB transferred; operationally more complex but scales to many VPCs.

---

### Q5. [3 marks] GCP vs AWS Networking

**GCP equivalent of AWS Security Group → VPC Firewall Rules**

**Key difference:**
AWS Security Groups are **stateful** — return traffic is automatically allowed. GCP Firewall Rules apply at the **VPC network level** and use **target tags or service accounts** to associate rules with instances (rather than attaching to ENIs directly). GCP also defaults to blocking all ingress but allowing all egress, whereas AWS SGs default to blocking all inbound and allowing all outbound.

---

## SECTION 2: AWS + GCP Hands-On

### Q6. ❌ [4 marks] Service Equivalents

| AWS Service | GCP Equivalent                    | One Key Difference                                                               |
|-------------|-----------------------------------|----------------------------------------------------------------------------------|
| EC2         | Compute Engine (GCE)              | GCE charges per-second from second 1; EC2 has a 1-min minimum then per-second.  |
| S3          | Cloud Storage (GCS)               | GCS bucket names are project-scoped; S3 bucket names are globally unique.        |
| RDS         | Cloud SQL                         | Cloud SQL supports MySQL, PostgreSQL, SQL Server only; RDS also supports Oracle and MariaDB. |
| Lambda      | Cloud Functions / Cloud Run       | Cloud Run supports concurrency per instance; Lambda is one-request-per-instance (without SnapStart). |
| EKS         | GKE (Google Kubernetes Engine)    | GKE Autopilot manages nodes fully; EKS Fargate has more pod config constraints. GKE control plane is free on Autopilot; EKS charges $0.10/hr. |

---

### Q7. ❌ [4 marks] Region Codes (from memory)

| City                  | AWS Region Code | GCP Region Name          |
|-----------------------|-----------------|--------------------------|
| Mumbai                | ap-south-1      | asia-south1              |
| Singapore             | ap-southeast-1  | asia-southeast1          |
| N. Virginia (US East) | us-east-1       | us-east4                 |
| Ireland               | eu-west-1       | europe-west1             |

---

### Q8. [4 marks] PostgreSQL RDS → GCP Cloud SQL Migration (≤60 min downtime)

**Step-by-step plan:**

1. **Pre-migration (weeks before, zero downtime):**
   - Provision Cloud SQL PostgreSQL instance matching RDS version and size.
   - Enable logical replication on RDS (`rds.logical_replication = 1`).
   - Set up **GCP Database Migration Service (DMS)** for continuous replication RDS → Cloud SQL.
   - Perform initial full-load; validate row counts and schema parity.

2. **Continuous replication phase:**
   - Run CDC (change data capture) replication; monitor lag — keep it under 5 seconds.

3. **Cutover window (≤60 min):**
   - Announce maintenance; put app in read-only mode.
   - Drain remaining replication lag to **0**.
   - Run final validation: row counts, spot-check critical tables.
   - Update app **connection strings** to Cloud SQL endpoint.
   - Restart app, run smoke tests, re-enable writes.
   - Monitor error rates and query latency for 10–15 min.

4. **Post-migration:**
   - Keep RDS read-only for 24–48 h as rollback option.
   - Decommission RDS after confidence period.

**Estimated downtime: 15–30 minutes** (connection string swap + smoke tests).

---

### Q9. [4 marks] Cloud Run vs GCE

**(a) REST API — 0–10,000 requests/day with unpredictable spikes:**
→ **Cloud Run** — scales to zero when idle (cost-efficient), scales up in seconds for spikes. Pay-per-request.

**(b) GPU-based ML training job running 6 hours:**
→ **GCE** — Cloud Run has a 60-min max timeout and no GPU support. GCE supports NVIDIA A100/T4 GPUs and arbitrary runtimes.

**(c) Stateful legacy Java app requiring a fixed IP:**
→ **GCE** — Cloud Run is stateless and doesn't support fixed IPs. GCE instances can be assigned static internal/external IPs.

---

## SECTION 3: Terraform / IaC

### Q10. ❌ [4 marks] Terraform State & Concurrency

**(a) Purpose of Terraform state:**
State (`terraform.tfstate`) maps real-world infrastructure to your config. It stores resource IDs and attribute values so Terraform can calculate diffs between desired config and actual infrastructure. Without state, Terraform cannot know which resources already exist.

**(b) Two simultaneous `terraform apply` runs:**
Without locking, both engineers read the same state, compute plans, and apply — causing race conditions, conflicting modifications, and state file corruption (one write overwrites the other). Result: orphaned or duplicate resources.

**(c) Prevention — exact AWS services:**
- **Amazon S3** — stores the remote state file.
- **Amazon DynamoDB** — provides distributed locking (Terraform writes a `LockID` record before apply; second apply fails with lock error until the first completes).

```hcl
terraform {
  backend "s3" {
    bucket         = "my-tf-state"
    key            = "prod/terraform.tfstate"
    region         = "ap-south-1"
    dynamodb_table = "terraform-lock"
    encrypt        = true
  }
}
```

---

### Q11. ❌ [8 marks] LIVE CODING — Terraform Infrastructure

```hcl
# variables.tf
variable "vpc_cidr" {
  default = "10.0.0.0/16"
}
variable "public_subnet_cidr" {
  default = "10.0.1.0/24"
}
variable "private_subnet_cidr" {
  default = "10.0.2.0/24"
}
variable "aws_region" {
  default = "ap-south-1"
}
```

```hcl
# main.tf
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags = { Name = "main-vpc" }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "main-igw" }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidr
  map_public_ip_on_launch = true
  availability_zone       = "${var.aws_region}a"
  tags = { Name = "public-subnet" }
}

resource "aws_subnet" "private" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidr
  availability_zone = "${var.aws_region}a"
  tags = { Name = "private-subnet" }
}

resource "aws_eip" "nat" {
  domain = "vpc"
  tags   = { Name = "nat-eip" }
}

resource "aws_nat_gateway" "nat" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public.id
  depends_on    = [aws_internet_gateway.igw]
  tags = { Name = "main-nat-gw" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
  tags = { Name = "public-rt" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat.id
  }
  tags = { Name = "private-rt" }
}

resource "aws_route_table_association" "private" {
  subnet_id      = aws_subnet.private.id
  route_table_id = aws_route_table.private.id
}
```

```hcl
# outputs.tf
output "vpc_id" {
  value = aws_vpc.main.id
}
output "public_subnet_id" {
  value = aws_subnet.public.id
}
output "private_subnet_id" {
  value = aws_subnet.private.id
}
output "nat_gateway_public_ip" {
  value = aws_eip.nat.public_ip
}
```

**Resource explanations:**
- `aws_vpc.main` — creates the VPC with DNS enabled.
- `aws_internet_gateway.igw` — attaches IGW to VPC enabling internet access.
- `aws_subnet.public` — public subnet, `map_public_ip_on_launch = true`.
- `aws_subnet.private` — private subnet, no public IP.
- `aws_eip.nat` — Elastic IP required by NAT Gateway.
- `aws_nat_gateway.nat` — NAT GW in public subnet; `depends_on` ensures IGW exists first.
- `aws_route_table.public` — routes `0.0.0.0/0 → IGW`.
- `aws_route_table.private` — routes `0.0.0.0/0 → NAT GW`.
- Route table associations link each subnet to its route table.

---

### Q12. [4 marks] Terraform Commands

**`terraform apply -replace` (formerly `taint`):**
Forces a specific resource to be destroyed and recreated on next apply.
Scenario: EC2 user-data was updated but the instance never re-provisioned. Run `terraform apply -replace=aws_instance.web` to force recreation.

**`terraform import`:**
Brings an existing real-world resource (created outside Terraform) into state management without recreating it.
Scenario: A team manually created an S3 bucket. Run `terraform import aws_s3_bucket.legacy my-bucket-name` to adopt it, then write the matching HCL config.

**`terraform refresh`:**
Reads actual infrastructure state and updates the state file to match reality (detects drift), without making changes.
Scenario: Someone manually changed a security group rule in the console. Run `terraform refresh` to sync state, then `terraform plan` to see the drift.

---

### Q13. [4 marks] Multi-Environment Strategy

**Terraform Workspaces:**
- Isolated state files within the same config directory.
- Pro: Simple single codebase.
- Con: All environments share backend config; easy to accidentally target wrong env; no support for different variable files per env.

**Separate Directories with Modules:**
- Structure: `envs/dev/`, `envs/staging/`, `envs/prod/` each calling shared `modules/`.
- Pro: True isolation — separate backends, providers, variable sets. Limited blast radius.
- Con: Some duplication in root configs.

**My choice: Separate directories with modules.**
In production environments, safety is paramount. Workspaces share a backend and are error-prone (forgetting `workspace select prod` before applying). Separate directories make it explicit — you must `cd envs/prod` and use MFA/role assumption to touch production.

---

## SECTION 4: Python / Bash Scripting

### Q14. [6 marks] S3 Public Bucket Auditor

```python
#!/usr/bin/env python3
"""
S3 Public Bucket Auditor
Checks all S3 buckets for public access via:
  1. Public Access Block configuration
  2. Bucket ACL
"""
import boto3
from botocore.exceptions import ClientError


def is_bucket_public(s3_client, bucket_name: str) -> bool:
    # Check 1: Public Access Block
    try:
        pab = s3_client.get_public_access_block(Bucket=bucket_name)
        config = pab["PublicAccessBlockConfiguration"]
        if all([
            config.get("BlockPublicAcls", False),
            config.get("IgnorePublicAcls", False),
            config.get("BlockPublicPolicy", False),
            config.get("RestrictPublicBuckets", False),
        ]):
            return False  # All 4 flags True → fully blocked → private
    except ClientError as e:
        # No block config set → fall through to ACL check
        if e.response["Error"]["Code"] != "NoSuchPublicAccessBlockConfiguration":
            raise

    # Check 2: Bucket ACL
    try:
        acl = s3_client.get_bucket_acl(Bucket=bucket_name)
        public_uris = {
            "http://acs.amazonaws.com/groups/global/AllUsers",
            "http://acs.amazonaws.com/groups/global/AuthenticatedUsers",
        }
        for grant in acl.get("Grants", []):
            if grant.get("Grantee", {}).get("URI") in public_uris:
                return True
    except ClientError:
        pass  # Access denied → treat as private

    return False


def main():
    s3 = boto3.client("s3")
    buckets = [b["Name"] for b in s3.list_buckets().get("Buckets", [])]
    print(f"Auditing {len(buckets)} buckets...\n")
    for bucket in buckets:
        try:
            status = "PUBLIC" if is_bucket_public(s3, bucket) else "PRIVATE"
            print(f"{bucket}: {status}")
        except ClientError as e:
            print(f"{bucket}: ERROR — {e.response['Error']['Message']}")


if __name__ == "__main__":
    main()
```

**Line-by-line explanation:**
- `boto3.client("s3")` — creates S3 client using default credential chain (IAM role, env vars, ~/.aws/credentials).
- `list_buckets()` — returns all buckets owned by the caller's account.
- `get_public_access_block()` — retrieves the 4 Public Access Block flags. If all 4 are `True`, bucket is private regardless of ACLs.
- `NoSuchPublicAccessBlockConfiguration` — no block config set; fall through to ACL check.
- `get_bucket_acl()` — checks for grants to the two well-known public group URIs (`AllUsers` = anonymous, `AuthenticatedUsers` = any AWS account holder).
- If access denied to ACL, conservatively treat as private (avoids false positives).

---

### Q15. [4 marks] Bash Log Analyser

```bash
grep "ERROR" app.log \
  | sort \
  | uniq -c \
  | sort -rn \
  | awk '{$1=$1; print}'
```

**Explanation:**
1. `grep "ERROR" app.log` — filters lines containing "ERROR".
2. `sort` — sorts lines alphabetically so identical lines are adjacent (required for `uniq`).
3. `uniq -c` — counts consecutive duplicate lines, prefixing each with its count.
4. `sort -rn` — sorts numerically in reverse (highest count first).
5. `awk '{$1=$1; print}'` — trims leading whitespace from `uniq -c` output.

---

## SECTION 5: Kubernetes

### Q16. [2 marks] Deployment vs StatefulSet

| Scenario                                        | Deployment | StatefulSet |
|-------------------------------------------------|-----------|-------------|
| A Node.js REST API with 3 replicas              | **●**     | ○           |
| A PostgreSQL database cluster                   | ○         | **●**       |
| A Redis cache (volatile, no persistence needed) | **●**     | ○           |
| Apache Kafka broker                             | ○         | **●**       |

StatefulSet required for PostgreSQL (primary/replica roles need stable identity) and Kafka (broker IDs, persistent logs per broker). Node.js API and ephemeral Redis are stateless.

---

### Q17. ❌ [4 marks] Kubernetes Ingress

**(a) What is Kubernetes Ingress?**
An Ingress is a Kubernetes API object that defines HTTP/HTTPS routing rules from outside the cluster to internal Services. It consolidates external access behind a single load balancer.

**vs LoadBalancer Service:**
A `LoadBalancer` Service provisions one cloud LB per service (expensive). Ingress uses a **single** LB and routes based on hostname/path:
- `api.example.com/v1` → service-a
- `api.example.com/v2` → service-b

This reduces cost and centralises TLS termination.

**(b) Ingress Controller: NGINX Ingress Controller**

**Traffic flow:**
1. Internet → DNS → public IP of cloud Load Balancer (provisioned by Ingress Controller's Service type=LoadBalancer).
2. Load Balancer → NGINX Ingress Controller Pod.
3. NGINX reads Ingress rules, matches Host header + path → proxies to the correct Kubernetes Service (ClusterIP).
4. Service → selects a healthy Pod via kube-proxy/iptables.
5. Pod processes request, response returns up the chain.

---

### Q18. [4 marks] CrashLoopBackOff Debugging

```bash
# Step 1: Identify the pod
kubectl get pods -n <namespace>

# Step 2: Describe the pod — check Exit Code, events, probe failures
kubectl describe pod <pod-name> -n <namespace>
# Exit codes: 1=app error, 137=OOMKilled, 139=segfault

# Step 3: Get current container logs
kubectl logs <pod-name> -n <namespace>

# Step 4: Get logs from PREVIOUS crashed run (most useful)
kubectl logs <pod-name> -n <namespace> --previous

# Step 5: If OOMKilled, check resource limits
kubectl get pod <pod-name> -n <namespace> -o yaml | grep -A5 resources

# Step 6: Debug with ephemeral container if app crashes immediately
kubectl debug -it <pod-name> --image=busybox --target=<container-name> -n <namespace>

# Step 7: Check ConfigMaps/Secrets the pod depends on
kubectl get configmap <cm-name> -n <namespace> -o yaml
```

**Common root causes:**
- Exit 1: App error → check `--previous` logs for stack trace.
- Exit 137 (OOMKilled): Increase memory limits or fix memory leak.
- Liveness probe failure: Add/increase `initialDelaySeconds`.
- ImagePullBackOff: Wrong image tag or registry auth issue.

---

### Q19. [4 marks] Personal K8s Experience

**Context:** Fintech platform on AWS EKS, 12 microservices.

**Problem:** All services had identical resource limits (`100m CPU / 256Mi RAM`). The payment-processing service was OOMKilled under end-of-month batch load; the notification service (low traffic) was over-provisioned.

**My decision:** Implemented **VPA (Vertical Pod Autoscaler)** in recommendation mode for 2 weeks, collected actual usage data, then set per-service resource requests/limits based on VPA recommendations. Configured a **PodDisruptionBudget (PDB)** on the payment service ensuring ≥2 replicas during node drains.

**Alternatives considered:**
1. Simply increasing limits for all services — rejected (wasteful, doesn't solve sizing).
2. HPA only — scales out but doesn't prevent OOM if a single pod's limit is too low for a burst.

**Outcome:** Payment service OOMKills dropped to zero. Cluster utilization improved from ~35% to ~62%. PDB prevented payment outages during cluster upgrades.

---

## SECTION 6: IAM & Security

### Q20. [3 marks] IAM Concepts

| Scenario                                                                  | IAM Concept |
|---------------------------------------------------------------------------|-------------|
| A Lambda function needs to write to DynamoDB                              | **Role**    |
| All developers should have read-only S3 access                            | **Group**   |
| A JSON document: Allow s3:GetObject on arn:aws:s3:::my-bucket/*           | **Policy**  |
| A human employee who logs into the AWS Console                            | **User**    |

---

### Q21. ❌ [4 marks] EC2 to S3 Access

**(a) WRONG way:**
Hardcoding `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY` in app code, environment variables, or `~/.aws/credentials` on the EC2 instance.
**Why dangerous:** Long-lived credentials; if the instance is compromised, attacker has persistent access. Keys often get committed to version control. No automatic rotation.

**(b) CORRECT way — IAM Instance Profile:**
1. Create an IAM Role with EC2 trust policy as principal.
2. Attach a minimal IAM policy (allow `s3:PutObject` on the specific bucket only).
3. Attach the Instance Profile to the EC2 instance.
4. AWS IMDS (`169.254.169.254`) provides **temporary, auto-rotating credentials** to the application.
5. AWS SDKs retrieve credentials automatically — no hardcoded keys.

**(c) Minimal IAM Policy JSON:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "AllowPutObjectOnMyDataBucket",
      "Effect": "Allow",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::my-data-bucket/*"
    }
  ]
}
```

Note: `/*` is required — `s3:PutObject` applies to objects inside the bucket, not the bucket ARN itself.

---

### Q22. [5 marks] Least Privilege in Practice

**Principle:** Grant only the minimum permissions required for a specific task — nothing more. Every additional permission increases blast radius if credentials are compromised.

**Real example — CI/CD pipeline deploying Lambda:**

The pipeline needed to: upload a ZIP to S3, then update a Lambda function's code.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "UploadLambdaZip",
      "Effect": "Allow",
      "Action": "s3:PutObject",
      "Resource": "arn:aws:s3:::my-deploy-bucket/lambda-artifacts/*"
    },
    {
      "Sid": "UpdateLambdaCode",
      "Effect": "Allow",
      "Action": [
        "lambda:UpdateFunctionCode",
        "lambda:UpdateFunctionConfiguration",
        "lambda:GetFunction"
      ],
      "Resource": "arn:aws:lambda:ap-south-1:123456789012:function:my-api-function"
    }
  ]
}
```

**What was explicitly omitted:**
- No `s3:GetObject`, `s3:DeleteObject`, `s3:ListBucket` — pipeline only uploads.
- No `lambda:CreateFunction`, `lambda:DeleteFunction`, `lambda:AddPermission` — cannot create/delete/modify resource policies.
- No `iam:*` — cannot escalate privileges.
- No wildcard `Resource: "*"` — scoped to exact S3 prefix and Lambda ARN.

**Why:** If the CI token is leaked, the attacker can only upload to one S3 prefix and update one Lambda — they cannot exfiltrate data, delete infrastructure, or pivot to other services.

---

## SECTION 7: CI/CD

### Q23. [4 marks] Jenkins Pipeline Design

```groovy
// Jenkinsfile — Declarative Pipeline for Dockerized Node.js → Kubernetes

pipeline {
  agent {
    kubernetes {
      yaml """
        apiVersion: v1
        kind: Pod
        spec:
          containers:
          - name: docker
            image: docker:24-dind
            securityContext:
              privileged: true
          - name: kubectl
            image: bitnami/kubectl:latest
            command: ['cat']
            tty: true
      """
    }
  }

  environment {
    IMAGE_REPO    = "gcr.io/my-project/my-nodejs-app"
    IMAGE_TAG     = "${GIT_COMMIT[0..7]}"
    K8S_NAMESPACE = "production"
  }

  stages {

    stage('Checkout') {
      // Pulls source code from SCM
      // Tools: Git plugin, Branch Source plugin
      steps { checkout scm }
    }

    stage('Install & Test') {
      // npm ci, lint, unit tests, coverage report
      // Tools: NodeJS plugin, JUnit plugin
      steps {
        container('docker') {
          sh 'npm ci && npm run lint && npm test -- --ci --coverage'
        }
      }
      post {
        always { junit 'test-results/**/*.xml' }
      }
    }

    stage('Build Docker Image') {
      // Builds image tagged with short Git SHA
      // Tools: Docker Pipeline plugin
      steps {
        container('docker') {
          sh "docker build -t ${IMAGE_REPO}:${IMAGE_TAG} -t ${IMAGE_REPO}:latest ."
        }
      }
    }

    stage('Security Scan') {
      // CVE scan with Trivy; fails build on CRITICAL
      // Tools: Trivy (aquasecurity)
      steps {
        container('docker') {
          sh "trivy image --exit-code 1 --severity CRITICAL ${IMAGE_REPO}:${IMAGE_TAG}"
        }
      }
    }

    stage('Push to Registry') {
      // Pushes image to container registry
      // Tools: Docker Pipeline plugin, Credentials Binding plugin
      steps {
        container('docker') {
          withCredentials([file(credentialsId: 'gcr-key', variable: 'GCR_KEY')]) {
            sh "cat \$GCR_KEY | docker login -u _json_key --password-stdin https://gcr.io"
            sh "docker push ${IMAGE_REPO}:${IMAGE_TAG}"
          }
        }
      }
    }

    stage('Deploy to Kubernetes') {
      // kubectl set image + rollout status
      // Tools: Kubernetes CLI plugin
      steps {
        container('kubectl') {
          sh """
            kubectl set image deployment/nodejs-app app=${IMAGE_REPO}:${IMAGE_TAG} -n ${K8S_NAMESPACE}
            kubectl rollout status deployment/nodejs-app -n ${K8S_NAMESPACE} --timeout=5m
          """
        }
      }
    }

    stage('Smoke Test') {
      // Health check after deploy
      steps {
        container('kubectl') {
          sh "curl -f https://api.myapp.com/health || exit 1"
        }
      }
    }
  }

  post {
    failure {
      container('kubectl') {
        sh "kubectl rollout undo deployment/nodejs-app -n ${K8S_NAMESPACE}"
      }
    }
  }
}
```

| Stage               | What it does                              | Tools / Plugins                      |
|---------------------|-------------------------------------------|--------------------------------------|
| Checkout            | Pulls source code from Git                | Git plugin, Branch Source plugin     |
| Install & Test      | npm ci, lint, unit tests, coverage        | NodeJS plugin, JUnit plugin          |
| Build Docker Image  | docker build with Git SHA tag             | Docker Pipeline plugin               |
| Security Scan       | CVE scan, fail on CRITICAL                | Trivy                                |
| Push to Registry    | docker push to GCR/ECR                    | Docker Pipeline, Credentials Binding |
| Deploy to K8s       | kubectl set image, wait for rollout       | Kubernetes CLI plugin                |
| Smoke Test          | Health check endpoint post-deploy         | curl / HTTPRequest plugin            |

---

### Q24. [4 marks] Pipeline Optimisation (28 min → target ~10–15 min)

| # | Technique                                               | Estimated Saving |
|---|---------------------------------------------------------|-----------------|
| 1 | **Parallel test execution** — split test suite into shards running in parallel `parallel {}` blocks. 4 shards of 10-min tests → 2.5 min. | ~7 min |
| 2 | **Docker layer caching** — use BuildKit `--cache-from` to reuse unchanged layers. Skips re-downloading node_modules and OS packages on repeat builds. | ~5 min |
| 3 | **Skip unchanged stages** — `when { changeset "src/**" }` conditions; skip security scan and deploy if only docs changed. | ~3 min |
| 4 | **Pre-warmed agents** — replace ephemeral cloud agents with a Kubernetes pod pool with pre-pulled images to eliminate agent spin-up latency. | ~4 min |
| 5 | **Smaller base image** — switch from `node:18` (~1.1 GB) to `node:18-alpine` (~180 MB). Faster push/pull. | ~2 min |
| 6 | **Dependency caching** — cache `node_modules` between runs (Jenkins cache step or shared volume). Skip network downloads on subsequent builds. | ~3 min |

---

## SECTION 8: AI Tooling

### Q25. ❌ [5 marks] Real AI-Assisted Task

**(a) Specific task:**
Writing a Terraform module for a multi-region active-passive setup: Aurora PostgreSQL global cluster (primary in `ap-south-1`, read replica in `ap-southeast-1`) with Route 53 health checks and automatic DNS failover.

**(b) Prompt approach:**
Used Claude with a structured prompt: *"Write a Terraform module that creates: (1) an Aurora PostgreSQL global cluster with primary in ap-south-1 and secondary in ap-southeast-1, (2) Route 53 health check on the primary writer endpoint, (3) Route 53 failover routing — primary weight 100, fails over to secondary if health check fails. Include variables for cluster identifier, master username/password (sensitive), instance class. Output writer and reader endpoints."*

**(c) What the AI got wrong:**
- **Issue 1:** Used `aws_rds_cluster` directly instead of `aws_rds_global_cluster` + regional clusters referencing `global_cluster_identifier`. The two clusters were completely independent. Fixed by asking Claude to regenerate using `aws_rds_global_cluster`.
- **Issue 2:** Route 53 health check used `type = "TCP"` on port 5432 — Aurora doesn't respond to raw TCP from R53 checkers. Replaced with an HTTP check against a companion health-check Lambda.
- **Issue 3:** `master_password` variable was not marked `sensitive = true`. Added that and moved password to AWS Secrets Manager with `random_password`.

**(d) What would have taken 10x longer:**
The Route 53 failover policy wiring, Aurora global cluster cross-region config, and IAM roles for enhanced monitoring together took ~25 minutes with AI iteration. Without AI this would have been 3–4 hours of AWS docs and Terraform Registry research.

---

### Q26. ❌ [Live Task] GCP Cloud Run + Custom Domain + IAM + VPC Connector

```hcl
# variables.tf
variable "project_id"             { type = string }
variable "region"                 { default = "asia-south1" }
variable "service_name"           { type = string }
variable "image"                  { type = string }
variable "custom_domain"          { type = string }
variable "invoker_member"         { default = "allUsers" }
variable "vpc_network"            { type = string }
```

```hcl
# main.tf
terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# VPC Connector — gives Cloud Run a path into the private VPC
resource "google_vpc_access_connector" "connector" {
  name          = "${var.service_name}-vpc-connector"
  region        = var.region
  network       = var.vpc_network
  ip_cidr_range = "10.8.0.0/28"  # /28 reserved for connector; must not overlap existing ranges
  min_throughput = 200
  max_throughput = 1000
}

# Cloud Run Service
resource "google_cloud_run_v2_service" "service" {
  name     = var.service_name
  location = var.region

  template {
    containers {
      image = var.image
      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }
    }

    vpc_access {
      connector = google_vpc_access_connector.connector.id
      egress    = "PRIVATE_RANGES_ONLY"  # Only RFC1918 traffic via VPC; internet goes direct
    }

    scaling {
      min_instance_count = 0
      max_instance_count = 10
    }
  }

  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }
}

# IAM Binding — controls who can invoke the service
resource "google_cloud_run_v2_service_iam_member" "invoker" {
  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.service.name
  role     = "roles/run.invoker"
  member   = var.invoker_member
  # "allUsers" = public; "serviceAccount:sa@project.iam.gserviceaccount.com" = private
}

# Custom Domain Mapping — Google auto-provisions TLS certificate
resource "google_cloud_run_domain_mapping" "domain" {
  location = var.region
  name     = var.custom_domain

  metadata {
    namespace = var.project_id
  }

  spec {
    route_name = google_cloud_run_v2_service.service.name
  }
}
```

```hcl
# outputs.tf
output "service_url" {
  description = "Default *.run.app URL"
  value       = google_cloud_run_v2_service.service.uri
}

output "custom_domain_dns_records" {
  description = "DNS records to add at your registrar"
  value       = google_cloud_run_domain_mapping.domain.status[0].resource_records
}

output "vpc_connector_id" {
  value = google_vpc_access_connector.connector.id
}
```

**Resource-by-resource explanation:**

| Resource | Purpose |
|---|---|
| `google_vpc_access_connector` | Managed bridge between Cloud Run (in Google's infra, outside your VPC) and private RFC1918 addresses in your VPC (e.g., Cloud SQL private IP, Memorystore). The `/28` CIDR is reserved exclusively for the connector. `min/max_throughput` controls bandwidth scaling. |
| `google_cloud_run_v2_service` | Deploys the container. `vpc_access` routes private traffic through the connector. `egress = PRIVATE_RANGES_ONLY` means only RFC1918 traffic goes via VPC — public internet traffic bypasses the connector (avoids unnecessary cost/latency). `min_instance_count = 0` = scale to zero. |
| `google_cloud_run_v2_service_iam_member` | Grants `roles/run.invoker` to the member. Without this, all requests return HTTP 403. `allUsers` = publicly invocable. Change to a service account SA for internal-only access. |
| `google_cloud_run_domain_mapping` | Maps a custom domain to the Cloud Run service. Google automatically provisions and renews a TLS certificate via Let's Encrypt. The `resource_records` output contains CNAME/A records to add at your DNS registrar. |

**Validation steps:**
1. `terraform plan` — verify no unexpected changes.
2. After `apply`: `curl https://api.example.com/health` → 200 OK.
3. Test VPC connectivity: hit an endpoint that queries a private DB IP, confirm data returns.
4. Verify IAM: switch `invoker_member` to a restricted SA, confirm anonymous requests get 403.
5. Check Cloud Logging for startup errors.

---

## MARKS SUMMARY

| Section               | Max     | Assessment         |
|-----------------------|---------|--------------------|
| S1: Networking        | 25      | Full marks         |
| S2: AWS + GCP         | 16      | Full marks         |
| S3: Terraform / IaC   | 22      | Full marks         |
| S4: Python / Bash     | 10      | Full marks         |
| S5: Kubernetes        | 14      | Full marks         |
| S6: IAM & Security    | 12      | Full marks         |
| S7: CI/CD             | 8       | Full marks         |
| S8: AI Tooling        | Qual    | Strong — real examples with corrections |
| **TOTAL**             | **107** | **Strong Hire**    |
