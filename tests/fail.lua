
function foo()
	error("test error")
end

function bar()
	foo()
end

local ok, msg = pcall(bar)
assert(not ok)
assert(msg == "test error")


