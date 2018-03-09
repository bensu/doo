#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "node :none" {
    run lein doo node node-none once
    assert_success
}

@test "node :advanced" {
    run lein doo node node-advanced once
    assert_success
}

@test "node :none fail" {
    run lein doo node node-none-fail once
    assert_failure
}

@test "node :advanced fail" {
    run lein doo node node-advanced-fail once
    assert_failure
}
