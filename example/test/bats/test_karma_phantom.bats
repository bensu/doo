#!/usr/bin/env bats
# -*- sh -*-

load test_helper

@test "karma-phantom :none" {
    run lein doo karma-phantom none-test once
    assert_success
}

@test "karma-phantom :advanced" {
    run lein doo karma-phantom advanced once
    assert_success
}

@test "karma-phantom :none fail" {
    run lein doo karma-phantom none-test-fail once
    assert_failure
}

@test "karma-phantom :advanced fail" {
    run lein doo karma-phantom advanced-fail once
    assert_failure
}
