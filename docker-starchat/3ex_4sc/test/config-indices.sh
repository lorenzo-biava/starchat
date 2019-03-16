# create the system indices in Elasticsearch
STARCHAT_URL="https://localhost:8443"
STARCHAT_AUTH="Authorization: Basic `echo -n 'admin:adminp4ssw0rd' | base64`"

INDEX_NAME="index_getjenny_english_0"

curl -vk -H "${STARCHAT_AUTH}" \
  -H "Content-Type: application/json" -X POST "${STARCHAT_URL}/system_index_management/create" | tee POST_system_index_management.log

echo
read -n1 -r -p "Press any key to continue..."

curl -vk -H "${STARCHAT_AUTH}" \
  -H "Content-Type: application/json" -X GET "${STARCHAT_URL}/system_index_management" | tee GET_system_index_management.log

echo
read -n1 -r -p "Press any key to continue..."

curl -vk -H "${STARCHAT_AUTH}" \
  -H "Content-Type: application/json" -X POST "${STARCHAT_URL}/${INDEX_NAME}/index_management/create" | tee POST_index_management.log

echo
read -n1 -r -p "Press any key to continue..."

curl -vk -H "${STARCHAT_AUTH}" \
  -H "Content-Type: application/json" -X GET "${STARCHAT_URL}/${INDEX_NAME}/index_management" | tee GET_index_management.log

echo
read -n1 -r -p "Press any key to continue..."
