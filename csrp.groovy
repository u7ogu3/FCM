import com.acm.main.ACM
import com.acm.clip.Clip

//Make sure search and replace strings are in the script
if (args.length == 2) {

	//Wait for a new item to be copied
	Clip clip = ACM.waitForNextClipItem()
	if (clip.isString){
		int oldId = clip.id
		String newString = clip.stringObject.replaceAll(args[0],args[1]) //Search and replace
		
		//Put new string in system clipboard and paste it
		ACM.setClipboardText(newString);
		clip = ACM.waitForNextClipItem()
		ACM.pasteClip(clip.id)
		
		//Delete old string
		ACM.deleteClip(oldId)
	}
} else {
	System.out.println("Usage: .csrp <search> <replace>");	
}