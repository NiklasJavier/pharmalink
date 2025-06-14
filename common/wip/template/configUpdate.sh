#!/usr/bin/env bash
#
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#

# This is a collection of bash functions used by different scripts
# for fetching, creating, and signing channel configuration updates.
# It requires 'jq' and 'configtxlator' for execution.

# imports
# TEST_NETWORK_HOME is set by network.sh (or other parent scripts) to the root
# of the 'test-network/' directory.
TEST_NETWORK_HOME=${TEST_NETWORK_HOME:-${PWD}}
. "${TEST_NETWORK_HOME}"/scripts/envVar.sh # Ensure envVar.sh is sourced for setGlobals

# fetchChannelConfig <org_short_name> <channel_id> <output_json_path>
# Writes the current channel config for a given channel to a JSON file.
fetchChannelConfig() {
  local ORG_SHORT_NAME="$1" # e.g., "de", "fr", "be", "es"
  local CHANNEL="$2"
  local OUTPUT="$3" # Full path for the output JSON file

  # Set global environment variables for the specified organization (e.g., CORE_PEER_MSPCONFIGPATH)
  setGlobals "${ORG_SHORT_NAME}"

  infoln "Fetching the most recent configuration block for channel '${CHANNEL}' as organization '${ORG_SHORT_NAME}'"
  set -x
  # Fetch the config block from the orderer.
  # Use orderer.navine.tech and the global ORDERER_CA from envVar.sh.
  peer channel fetch config "${TEST_NETWORK_HOME}/channel-artifacts/config_block.pb" \
    -o "orderer.navine.tech:7050" \
    --ordererTLSHostnameOverride orderer.navine.tech \
    -c "${CHANNEL}" \
    --tls --cafile "${ORDERER_CA}" # ORDERER_CA is a global from envVar.sh
  res=$?
  { set +x; } 2>/dev/null
  verifyResult $res "Failed to fetch config block from channel '${CHANNEL}'"

  infoln "Decoding config block to JSON and isolating config to ${OUTPUT}"
  set -x
  # Decode the config block to JSON format
  configtxlator proto_decode --input "${TEST_NETWORK_HOME}/channel-artifacts/config_block.pb" \
    --type common.Block --output "${TEST_NETWORK_HOME}/channel-artifacts/config_block.json"

  # Use jq to extract the channel configuration from the block and save it to the output JSON file
  jq '.data.data[0].payload.data.config' "${TEST_NETWORK_HOME}/channel-artifacts/config_block.json" >"${OUTPUT}"
  res=$?
  { set +x; } 2>/dev/null
  verifyResult $res "Failed to parse channel configuration using jq. Make sure you have jq installed and the config block is valid."
}

# createConfigUpdate <channel_id> <original_config.json> <modified_config.json> <output.pb>
# Takes an original and modified config, and produces the config update transaction (protobuf)
# which transitions between the two.
createConfigUpdate() {
  local CHANNEL="$1"
  local ORIGINAL="$2" # Full path to original_config.json
  local MODIFIED="$3" # Full path to modified_config.json
  local OUTPUT="$4"   # Full path for the output config update protobuf file (.pb)

  infoln "Creating config update transaction for channel '${CHANNEL}'"
  set -x
  # Encode original and modified JSON configs to protobuf
  configtxlator proto_encode --input "${ORIGINAL}" --type common.Config --output "${TEST_NETWORK_HOME}/channel-artifacts/original_config.pb"
  configtxlator proto_encode --input "${MODIFIED}" --type common.Config --output "${TEST_NETWORK_HOME}/channel-artifacts/modified_config.pb"

  # Compute the difference between original and modified configs as a config update
  configtxlator compute_update --channel_id "${CHANNEL}" \
    --original "${TEST_NETWORK_HOME}/channel-artifacts/original_config.pb" \
    --updated "${TEST_NETWORK_HOME}/channel-artifacts/modified_config.pb" \
    --output "${TEST_NETWORK_HOME}/channel-artifacts/config_update.pb"

  # Decode the config update to JSON (for wrapping in an envelope)
  configtxlator proto_decode --input "${TEST_NETWORK_HOME}/channel-artifacts/config_update.pb" \
    --type common.ConfigUpdate --output "${TEST_NETWORK_HOME}/channel-artifacts/config_update.json"

  # Wrap the config update in a common.Envelope structure
  echo '{"payload":{"header":{"channel_header":{"channel_id":"'${CHANNEL}'", "type":2}},"data":{"config_update":'$(cat "${TEST_NETWORK_HOME}/channel-artifacts/config_update.json")'}}}' | jq . > "${TEST_NETWORK_HOME}/channel-artifacts/config_update_in_envelope.json"

  # Encode the envelope to protobuf for final transaction
  configtxlator proto_encode --input "${TEST_NETWORK_HOME}/channel-artifacts/config_update_in_envelope.json" \
    --type common.Envelope --output "${OUTPUT}"
  res=$?
  { set +x; } 2>/dev/null
  verifyResult $res "Failed to create config update transaction for channel '${CHANNEL}'"
  successln "Config update transaction for channel '${CHANNEL}' created at ${OUTPUT}"
}

# signConfigtxAsPeerOrg <org_short_name> <configtx.pb_file_path>
# Set the peerOrg admin of an org and sign the config update.
# The signed config update is typically stored as a new .pb file for multi-signature scenarios.
signConfigtxAsPeerOrg() {
  local ORG_SHORT_NAME="$1"
  local CONFIGTXFILE="$2" # Full path to the config update protobuf file (.pb)

  # Set environment variables for the specified organization's admin
  setGlobals "${ORG_SHORT_NAME}"

  infoln "Signing config update transaction with organization '${ORG_SHORT_NAME}' admin"
  set -x
  peer channel signconfigtx -f "${CONFIGTXFILE}"
  res=$?
  { set +x; } 2>/dev/null
  verifyResult $res "Failed to sign config update transaction as organization '${ORG_SHORT_NAME}'"
  successln "Config update transaction signed by organization '${ORG_SHORT_NAME}'"
}

# Main execution logic (This script is usually sourced or called by other scripts)
# No direct execution block, as it defines functions to be used externally.