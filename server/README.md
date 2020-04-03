# Install
* kubectl
* minikube
* skaffold
 1. curl -Lo skaffold https://storage.googleapis.com/skaffold/releases/v1.0.0/skaffold-darwin-amd64 && chmod +x skaffold && sudo mv skaffold /usr/local/bin
* docker mmm
 1. Install Docker Desktop
 2. brew install docker-credential-helper

# Create cluster in gcloud
  1. gcloud beta container --project "tracetogether-273112" clusters create "cluster-1" --zone "europe-north1-a" --no-enable-basic-auth --release-channel "regular" --machine-type "n1-standard-1" --image-type "COS" --disk-type "pd-standard" --disk-size "100" --metadata disable-legacy-endpoints=true --scopes "https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append" --num-nodes "3" --enable-stackdriver-kubernetes --enable-ip-alias --network "projects/tracetogether-273112/global/networks/default" --subnetwork "projects/tracetogether-273112/regions/europe-north1/subnetworks/default" --default-max-pods-per-node "110" --no-enable-master-authorized-networks --addons HorizontalPodAutoscaling,HttpLoadBalancing --enable-autoupgrade --enable-autorepair

# Deploy
 1. gcloud container clusters get-credentials cluster-1 --zone europe-north1-a 
 2. kubectl run tracetogether --image registry.hub.docker.com/jontejj/tracetogether:latest --port=8080 
 3. kubectl expose deployment tracetogether --type="LoadBalancer"  
 4. kubectl get service tracetogether --watch
