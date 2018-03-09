#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "slimer :none" {
    run lein doo slimer none-test once
    assert_success
}

@test "slimer :advanced" {
    run lein doo slimer advanced once
    assert_success
}
