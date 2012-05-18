#What is Mobi Blogger
<a href="http://netmera.com">Netmera</a> Platform is a cloud platform (PaaS) optimized for mobile applications. Netmera offers a cloud based content &amp; data repository. With simple APIs and mobile SDKs it is easy to store, retrieve, search and query data &amp; content on the cloud.
Mobi Blogger is a sample mobile application, developed for android systems, and uses Netmera APIs for server side interactions. It is a basic mobile blogger application, where users can add, edit, display and delete their blogs with server side operations. Since this is a sample project, the users may manage text and photo type contents.
#Usage

	private void initAPI(){
		NetmeraClient.init(getApplicationContext(), GeneralConstants.SECURITY_KEY);
	}

##Content Context Object
Storing content in Netmera mobile service is done by creating ContentContext object which contains key-value pairs of data.


##Content Service Ojbect
ContentService object is used to retrieve contents by its search and get methods. Many query options defined to help finding exact object easily. There are also search methods which gets the contents with the given range of locations.

###Create Content
Following code is used to create content object. First a content context object is created with a key, then several data are added to this object as key-value pairs. Data is added when create() function is called.

		ContentContext cc = new ContentContext(GeneralConstants.DATA_TABLE_NAME);
		cc.add(GeneralConstants.KEY_TITLE, title.getText().toString());
		cc.add(GeneralConstants.KEY_DESCRIPTION, description.getText().toString());
		cc.create();

For adding photo object, the procedure is a little different, image is set as NetmeraMedia object.

		byte[] bytes = HttpUtils.toByteArray(new File (path));	
		NetmeraMedia file = new NetmeraMedia(bytes);
		cc.add(GeneralConstants.KEY_PHOTOS + i, file);
		cc.create();

###Delete Content
Following code can be used to delete content from the Netmera repository. In order to delete content either set path to find and delete content or first call get() or search() methods and then delete the retrieved ContentContext object.

		path = (String) getIntent().getExtras().get(GeneralConstants.KEY_PATH);
		ContentContext cc = new ContentContext(GeneralConstants.DATA_TABLE_NAME);
		cc.setPath(path);
		cc.delete();
###Get Content
In order to get the data you need to know the path. In Netmera repository each content has a unique path.

		ContentService service = new ContentService(GeneralConstants.DATA_TABLE_NAME);
		service.setPath(path);		
		ContentContext ctx = service.get();
		titleText.setText(ctx.getString(GeneralConstants.KEY_TITLE));
		contentText.setText(ctx.getString(GeneralConstants.KEY_DESCRIPTION));
	
For getting photo from the repository, following code can be used:

		NetmeraMedia media = ctx.getNetmeraMedia("file" + mediaCount);
		String url = media.getUrl(NetmeraMedia.PhotoSize.SMALL);
		byte[] imageBytes = media.getData();
		Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
	
###Search
The following code searches the content and returns the list of ContentContext objects. If you set searchText then it will search the content repository and retrieve the results which contains the searchText. If searchText is not set then it returns all the content that matches with the objectName.

		ContentService cs = new ContentService(GeneralConstants.DATA_TABLE_NAME);
		List<ContentContext> ccList = cs.search();

###Update Content
First the object to be edited is fetched, then the edit operation is just like adding. The only difference is that update() function is called instead of create() function.

		path = (String) getIntent().getExtras().get(GeneralConstants.KEY_PATH);
		ContentContext cc = new ContentContext(GeneralConstants.DATA_TABLE_NAME);
		cc.setPath(path);
		cc.add(GeneralConstants.KEY_TITLE, titleText);
		cc.add(GeneralConstants.KEY_DESCRIPTION, descriptionText);
		String path = photoPaths.get(i-numberOfPhotos);
		byte[] bytes = HttpUtils.toByteArray(new File (path));	
		NetmeraMedia file = new NetmeraMedia(bytes);
		cc.add(GeneralConstants.KEY_PHOTOS, file);


# TODO