# RDS SSL Certificate Setup Instructions

## Step 1: Download the AWS RDS Certificate

Run this command from the project root directory:

```bash
curl -o src/main/resources/certs/global-bundle.pem https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem
```

Alternatively, copy it from your EC2 instance:

```bash
scp your-ec2-instance:/certs/global-bundle.pem src/main/resources/certs/
```

## Step 2: Verify the certificate file exists

```bash
ls -lh src/main/resources/certs/global-bundle.pem
```

You should see a file that's approximately 10-20 KB.

## Step 3: Rebuild and redeploy

```bash
# Build the application
mvn clean package

# Build the Docker image
docker build -t your-registry/itkannadigaru:latest .

# Push to your registry
docker push your-registry/itkannadigaru:latest

# Update the image reference in deployment.yml (line 17)
# Then apply the deployment
kubectl apply -f deployment.yml
```

## What was changed:

1. **application.properties** - Updated:
   - Database URL to include `sslmode=verify-full&sslrootcert=/certs/global-bundle.pem`
   - Password changed from "password" to "Manoj7100"

2. **Dockerfile** - Added:
   - Creation of `/certs` directory
   - Copying of certificate from project to `/certs/global-bundle.pem` in container

3. **Created** `src/main/resources/certs/` directory for the certificate file

## Troubleshooting

If you still get connection errors:

1. Verify the RDS instance is running:
   ```bash
   aws rds describe-db-instances --db-instance-identifier itkannadigaru
   ```

2. Check security groups allow traffic from your EKS cluster

3. Test connection from a pod:
   ```bash
   kubectl run -it --rm debug --image=postgres:15 --restart=Never -- bash
   PGPASSWORD="Manoj7100" psql -h itkannadigaru.cfoqwaayg09s.us-west-2.rds.amazonaws.com -p 5432 -U postgres -d postgres
   ```
