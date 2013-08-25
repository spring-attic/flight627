define("orion/editor/javaContentAssist", [], function() {
	function JavaContentAssistProvider() {}
	
	JavaContentAssistProvider.prototype =
	{
		computeProposals: function(buffer, offset, context) {
			var jProps = this.__javaObject.getProposals(context.line, context.prefix, offset);
			var proposals = JSON.parse(jProps);			
			return proposals;
		}
	}
	
	return {
		JavaContentAssistProvider: JavaContentAssistProvider
	};
});