#!/usr/bin/env bash

# Define three lists
seeds=(0 1)
nodes=(20 40 60 80 100)
baseline=(true false)
strategy=("heuristic" "optimal")

# Available cores minus one
num_cores=$(nproc --all)
num_cores=$((num_cores - 2))

run_simulation() {
  local seed=$1
  local node=$2
  local bl=$3
  local str=$4
  echo "Running simulation with seed=$seed, nodes=$node, baseline=$bl, strategy=$str"
  SEED=$seed NODES=$node IS_BASELINE=$bl DEPLOYMENT_STRATEGY=$str ./gradlew runPulverizedPrologPlacerManualCustom
}

export -f run_simulation

# Run the simulation in parallel
LC_ALL=C parallel --jobs "$num_cores" run_simulation ::: "${seeds[@]}" ::: "${nodes[@]}" ::: "${baseline[@]}" ::: "${strategy[@]}"
